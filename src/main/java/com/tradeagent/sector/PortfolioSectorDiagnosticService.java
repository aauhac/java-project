package com.tradeagent.sector;

import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.PortfolioRepository;
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

    private final PortfolioRepository portfolioRepository;
    private final SectorMasterRepository sectorMasterRepository;
    private final SectorAnalysisService sectorAnalysisService;
    private final SectorTrendAnalysisService sectorTrendAnalysisService;
    private final SectorFeedbackService sectorFeedbackService;

    public PortfolioSectorDiagnosticService(PortfolioRepository portfolioRepository,
                                            SectorMasterRepository sectorMasterRepository,
                                            SectorAnalysisService sectorAnalysisService,
                                            SectorTrendAnalysisService sectorTrendAnalysisService,
                                            SectorFeedbackService sectorFeedbackService) {
        this.portfolioRepository = portfolioRepository;
        this.sectorMasterRepository = sectorMasterRepository;
        this.sectorAnalysisService = sectorAnalysisService;
        this.sectorTrendAnalysisService = sectorTrendAnalysisService;
        this.sectorFeedbackService = sectorFeedbackService;
    }

    public PortfolioSectorDiagnosticDto diagnose(Long userId) {
        List<SectorExposureDto> exposures = getMixedExposureBreakdown(userId, sectorAnalysisService.getLatestSectorScores());
        BigDecimal strongExposure = sumExposureByStatus(exposures, "STRONG");
        BigDecimal weakExposure = sumExposureByStatus(exposures, "WEAK");
        PortfolioSectorDiagnosticDto draft = new PortfolioSectorDiagnosticDto(
                strongExposure,
                weakExposure,
                "",
                exposures
        );
        return new PortfolioSectorDiagnosticDto(
                strongExposure,
                weakExposure,
                sectorFeedbackService.buildSectorFeedback(draft),
                exposures
        );
    }

    public PortfolioTrendMatchDto calculateTrendMatch(Long userId, LocalDate date) {
        List<SectorExposureDto> exposures = getTrendExposureBreakdown(userId, sectorTrendAnalysisService.getTrendScores(date));
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
        return getMixedExposureBreakdown(userId, sectorAnalysisService.getLatestSectorScores());
    }

    private List<SectorExposureDto> getMixedExposureBreakdown(Long userId, List<SectorScoreDto> scores) {
        Map<String, SectorScoreDto> sectorScoreMap = scores.stream()
                .collect(Collectors.toMap(SectorScoreDto::sectorCode, Function.identity()));
        return buildExposureBreakdown(userId, sectorScoreMap, SectorScoreDto::totalSectorScore, SectorScoreDto::status);
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

        Map<String, String> sectorNameMap = sectorMasterRepository.findAllByOrderBySectorCodeAsc().stream()
                .collect(Collectors.toMap(SectorMaster::getSectorCode, SectorMaster::getSectorName));

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
                    T sectorScore = sectorScoreMap.get(entry.getKey());
                    BigDecimal totalSectorScore = sectorScore != null
                            ? scoreExtractor.apply(sectorScore).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                    String status = sectorScore != null ? statusExtractor.apply(sectorScore) : "NEUTRAL";
                    return new SectorExposureDto(
                            entry.getKey(),
                            sectorNameMap.getOrDefault(entry.getKey(), entry.getKey()),
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
}
