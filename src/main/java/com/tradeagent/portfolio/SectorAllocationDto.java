package com.tradeagent.portfolio;

import java.math.BigDecimal;

public record SectorAllocationDto(
        String sectorCode,
        String sectorName,
        BigDecimal marketValue,
        BigDecimal allocationRate
) {
}
