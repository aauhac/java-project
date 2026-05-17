package com.tradeagent.sector;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.PortfolioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PortfolioSectorDiagnosticService {

    private static final BigDecimal STRONG_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal WEAK_THRESHOLD = BigDecimal.valueOf(40);

    private final PortfolioRepository portfolioRepository;
    private final SectorMasterRepository sectorMasterRepository;
    private final SectorAnalysisService sectorAnalysisService;
    private final SectorFeedbackService sectorFeedbackService;

    public PortfolioSectorDiagnosticService(PortfolioRepository portfolioRepository,
                                            SectorMasterRepository sectorMasterRepository,
                                            SectorAnalysisService sectorAnalysisService,
                                            SectorFeedbackService sectorFeedbackService) {
        this.portfolioRepository = portfolioRepository;
        this.sectorMasterRepository = sectorMasterRepository;
        this.sectorAnalysisService = sectorAnalysisService;
        this.sectorFeedbackService = sectorFeedbackService;
    }

    public PortfolioSectorDiagnosticDto diagnose(Long userId) {
        List<SectorExposureDto> exposures = getSectorExposureBreakdown(userId);
        BigDecimal strongExposure = calculateStrongExposure(userId);
        BigDecimal weakExposure = calculateWeakExposure(userId);
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

    public BigDecimal calculateStrongExposure(Long userId) {
        return getSectorExposureBreakdown(userId).stream()
                .filter(exposure -> exposure.sectorScore().compareTo(STRONG_THRESHOLD) >= 0)
                .map(SectorExposureDto::portfolioRate)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWeakExposure(Long userId) {
        return getSectorExposureBreakdown(userId).stream()
                .filter(exposure -> exposure.sectorScore().compareTo(WEAK_THRESHOLD) <= 0)
                .map(SectorExposureDto::portfolioRate)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public List<SectorExposureDto> getSectorExposureBreakdown(Long userId) {
        List<PortfolioPosition> positions = portfolioRepository.findByUserId(userId);
        if (positions.isEmpty()) {
            return List.of();
        }

        List<SectorScoreDto> latestScores = sectorAnalysisService.getLatestSectorScores();
        Map<String, SectorScoreDto> sectorScoreMap = latestScores.stream()
                .collect(Collectors.toMap(SectorScoreDto::sectorCode, Function.identity()));
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
                    SectorScoreDto sectorScore = sectorScoreMap.get(entry.getKey());
                    BigDecimal totalSectorScore = sectorScore != null
                            ? sectorScore.totalSectorScore().setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
                    String status = sectorScore != null ? sectorScore.status() : "NEUTRAL";
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

}
