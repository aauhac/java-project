package com.tradeagent.sector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
