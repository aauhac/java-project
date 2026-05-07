package com.tradeagent.evaluation;

import java.math.BigDecimal;

public record DecisionSummaryDto(
        BigDecimal averageEntryScore,
        BigDecimal averageExitScore,
        BigDecimal averageRiskScore,
        BigDecimal averageDiversificationScore,
        BigDecimal averageSectorFitScore,
        BigDecimal averageTotalScore,
        String mainWeakness
) {
}
