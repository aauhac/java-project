package com.tradeagent.sector;

import java.math.BigDecimal;

public record SectorExposureDto(
        String sectorCode,
        String sectorName,
        BigDecimal portfolioRate,
        BigDecimal sectorScore,
        String status
) {
}
