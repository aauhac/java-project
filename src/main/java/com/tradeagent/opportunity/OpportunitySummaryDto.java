package com.tradeagent.opportunity;

public record OpportunitySummaryDto(
        int missedCount,
        int avoidedCount,
        int soldTooEarlyCount,
        int heldTooLongCount
) {
}
