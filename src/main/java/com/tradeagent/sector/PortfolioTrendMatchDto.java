package com.tradeagent.sector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PortfolioTrendMatchDto(
        LocalDate matchDate,
        BigDecimal trendMatchScore,
        BigDecimal strongExposure,
        BigDecimal weakExposure,
        String message,
        List<SectorExposureDto> exposures
) {
}
