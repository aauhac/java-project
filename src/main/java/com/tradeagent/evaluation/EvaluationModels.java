package com.tradeagent.evaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class EvaluationModels {

    private EvaluationModels() {
    }

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

    public record TradeEvaluationDto(
            Long tradeHistoryId,
            BigDecimal entryScore,
            BigDecimal exitScore,
            BigDecimal riskScore,
            BigDecimal diversificationScore,
            BigDecimal sectorFitScore,
            BigDecimal totalScore,
            String feedback,
            LocalDateTime evaluatedAt,
            List<ScoreDetailDto> scoreDetails
    ) {
    }

    public record ScoreDetailDto(
            String name,
            BigDecimal score,
            String comment
    ) {
    }
}
