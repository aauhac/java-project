package com.tradeagent.sector;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSectorDiagnosticDto(
        BigDecimal strongExposure,
        BigDecimal weakExposure,
        String message,
        List<SectorExposureDto> exposures
) {
}
