package com.tradeagent.opportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class OpportunityApiModels {

    private OpportunityApiModels() {
    }

    public record OpportunityDto(
            String type,
            String symbol,
            String sectorCode,
            BigDecimal changeRate,
            String reason,
            String feedback,
            LocalDateTime detectedAt
    ) {
    }

    public record OpportunitySummaryDto(
            int missedCount,
            int avoidedCount,
            int soldTooEarlyCount,
            int heldTooLongCount
    ) {
    }

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
}
