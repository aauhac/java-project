package com.tradeagent.opportunity;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BetterTimingDto(
        String patternType,
        String symbol,
        String sectorCode,
        LocalDate baseDate,
        BigDecimal actualPrice,
        BigDecimal referencePrice,
        BigDecimal changeRate,
        String reason,
        String feedback
) {
}
