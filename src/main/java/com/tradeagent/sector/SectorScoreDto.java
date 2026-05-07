package com.tradeagent.sector;

import java.math.BigDecimal;
import java.time.LocalDate;

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
