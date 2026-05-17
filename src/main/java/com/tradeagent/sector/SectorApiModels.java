package com.tradeagent.sector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class SectorApiModels {

    private SectorApiModels() {
    }

    public record NewsEventDto(
            String sectorCode,
            String symbol,
            String title,
            String source,
            String url,
            BigDecimal toneScore,
            LocalDateTime publishedAt
    ) {
    }

    public record SectorScoreDto(
            String sectorCode,
            String sectorName,
            LocalDate scoreDate,
            BigDecimal newsVolumeScore,
            BigDecimal newsToneScore,
            BigDecimal priceMomentumScore,
            BigDecimal volumeSpikeScore,
            BigDecimal breadthScore,
            BigDecimal totalSectorScore,
            String status
    ) {
    }

    public record SectorTrendDto(
            String sectorCode,
            String sectorName,
            LocalDate scoreDate,
            Integer articleCount,
            BigDecimal avgToneScore,
            BigDecimal newsVolumeScore,
            BigDecimal newsToneScore,
            BigDecimal keywordStrengthScore,
            BigDecimal totalSectorScore,
            String status,
            LocalDateTime analyzedAt
    ) {
    }

    public record SectorExposureDto(
            String sectorCode,
            String sectorName,
            BigDecimal portfolioRate,
            BigDecimal sectorScore,
            String status
    ) {
    }

    public record PortfolioSectorDiagnosticDto(
            BigDecimal strongExposure,
            BigDecimal weakExposure,
            String message,
            List<SectorExposureDto> exposures
    ) {
    }

    public record PortfolioTrendMatchDto(
            LocalDate matchDate,
            BigDecimal trendMatchScore,
            BigDecimal strongExposure,
            BigDecimal weakExposure,
            String message,
            List<SectorExposureDto> exposures
    ) {
    }
}
