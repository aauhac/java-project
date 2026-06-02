package com.tradeagent.sector;

import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.PortfolioRepository;
import com.tradeagent.sector.SectorApiModels.PortfolioSectorDiagnosticDto;
import com.tradeagent.sector.SectorApiModels.PortfolioTrendMatchDto;
import com.tradeagent.sector.SectorApiModels.SectorExposureDto;
import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PortfolioSectorDiagnosticService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String NO_TREND_DATA_MESSAGE = "저장된 동향 분석 데이터가 없습니다. 동향 분석 버튼을 눌러주세요.";

    private final PortfolioRepository portfolioRepository;
    private final SectorGkgTrendService sectorGkgTrendService;

    public PortfolioSectorDiagnosticService(PortfolioRepository portfolioRepository,
                                            SectorGkgTrendService sectorGkgTrendService) {
        this.portfolioRepository = portfolioRepository;
        this.sectorGkgTrendService = sectorGkgTrendService;
    }

    public PortfolioSectorDiagnosticDto diagnose(Long userId) {
        List<SectorTrendDto> scores = sectorGkgTrendService.getTrendScoresForDate(LocalDate.now());
        List<SectorExposureDto> exposures = getTrendExposureBreakdown(userId, scores);
        BigDecimal strongExposure = sumExposureByStatus(exposures, "STRONG");
        BigDecimal weakExposure = sumExposureByStatus(exposures, "WEAK");
        return new PortfolioSectorDiagnosticDto(
                strongExposure,
                weakExposure,
                buildDiagnosticMessage(strongExposure, weakExposure, exposures),
                exposures
        );
    }

    public PortfolioTrendMatchDto calculateTrendMatch(Long userId, LocalDate date) {
        List<SectorTrendDto> scores = sectorGkgTrendService.getTrendScoresForDate(date);
        if (scores.isEmpty()) {
            return new PortfolioTrendMatchDto(
                    date != null ? date : LocalDate.now(),
                    ZERO,
                    ZERO,
                    ZERO,
                    NO_TREND_DATA_MESSAGE,
                    getTrendExposureBreakdown(userId, List.of())
            );
        }
        return calculateTrendMatch(userId, date, scores);
    }

    public PortfolioTrendMatchDto calculateTrendMatch(Long userId, LocalDate date, List<SectorTrendDto> scores) {
        List<SectorExposureDto> exposures = getTrendExposureBreakdown(userId, scores);
        BigDecimal strongExposure = sumExposureByStatus(exposures, "STRONG");
        BigDecimal weakExposure = sumExposureByStatus(exposures, "WEAK");
        BigDecimal trendMatchScore = exposures.stream()
                .map(exposure -> exposure.portfolioRate()
                        .multiply(exposure.sectorScore())
                        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        return new PortfolioTrendMatchDto(
                date != null ? date : LocalDate.now(),
                trendMatchScore,
                strongExposure,
                weakExposure,
                buildTrendMatchMessage(trendMatchScore, strongExposure, weakExposure),
                exposures
        );
    }

    public List<SectorExposureDto> getSectorExposureBreakdown(Long userId) {
        return getTrendExposureBreakdown(userId, sectorGkgTrendService.getTrendScoresForDate(LocalDate.now()));
    }

    private List<SectorExposureDto> getTrendExposureBreakdown(Long userId, List<SectorTrendDto> scores) {
        Map<String, SectorTrendDto> sectorScoreMap = scores.stream()
                .collect(Collectors.toMap(SectorTrendDto::sectorCode, Function.identity()));
        return buildExposureBreakdown(userId, sectorScoreMap, SectorTrendDto::totalSectorScore, SectorTrendDto::status);
    }

    private <T> List<SectorExposureDto> buildExposureBreakdown(Long userId,
                                                               Map<String, T> sectorScoreMap,
                                                               Function<T, BigDecimal> scoreExtractor,
                                                               Function<T, String> statusExtractor) {
        List<PortfolioPosition> positions = portfolioRepository.findByUserId(userId);
        if (positions.isEmpty()) {
            return List.of();
        }

        BigDecimal totalExposure = positions.stream()
                .map(PortfolioPosition::getTotalBuyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalExposure.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }

        Map<String, BigDecimal> sectorExposure = positions.stream()
                .collect(Collectors.groupingBy(
                        PortfolioPosition::getSectorCode,
                        Collectors.reducing(BigDecimal.ZERO, PortfolioPosition::getTotalBuyAmount, BigDecimal::add)
                ));

        return sectorExposure.entrySet().stream()
                .map(entry -> {
                    BigDecimal portfolioRate = entry.getValue()
                            .divide(totalExposure, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
                    String sectorCode = entry.getKey();
                    T sectorScore = sectorScoreMap.get(sectorCode);
                    BigDecimal totalSectorScore = sectorScore != null
                            ? scoreExtractor.apply(sectorScore).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    String status = sectorScore != null ? statusExtractor.apply(sectorScore) : "NEUTRAL";
                    return new SectorExposureDto(
                            sectorCode,
                            SectorConstants.nameOf(sectorCode),
                            portfolioRate,
                            totalSectorScore,
                            status
                    );
                })
                .sorted(Comparator.comparing(SectorExposureDto::portfolioRate).reversed())
                .toList();
    }

    private BigDecimal sumExposureByStatus(List<SectorExposureDto> exposures, String status) {
        return exposures.stream()
                .filter(exposure -> status.equals(exposure.status()))
                .map(SectorExposureDto::portfolioRate)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String buildTrendMatchMessage(BigDecimal trendMatchScore, BigDecimal strongExposure, BigDecimal weakExposure) {
        if (trendMatchScore.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "현재 포트폴리오는 최근 강한 섹터 흐름과 잘 맞고 있습니다.";
        }
        if (weakExposure.compareTo(strongExposure) > 0) {
            return "약한 섹터 노출이 더 높아 최근 뉴스 동향과의 일치도가 낮습니다.";
        }
        return "중립적인 흐름입니다. 강한 섹터 노출을 조금 더 늘릴 여지가 있습니다.";
    }

    private String buildDiagnosticMessage(BigDecimal strongExposure, BigDecimal weakExposure, List<SectorExposureDto> exposures) {
        if (exposures.isEmpty()) {
            return "포트폴리오 보유 종목이 없어 섹터 비중을 계산할 수 없습니다.";
        }
        if (strongExposure.compareTo(weakExposure) > 0) {
            return "현재 포트폴리오는 상위 섹터에 상대적으로 더 많이 배분되어 있습니다.";
        }
        if (strongExposure.compareTo(weakExposure) < 0) {
            return "현재 포트폴리오는 하위 섹터 비중이 더 높아 재점검이 필요합니다.";
        }
        return "현재 포트폴리오는 상위/하위 섹터 비중이 유사합니다.";
    }
}
