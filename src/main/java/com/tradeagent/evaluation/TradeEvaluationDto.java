package com.tradeagent.evaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
