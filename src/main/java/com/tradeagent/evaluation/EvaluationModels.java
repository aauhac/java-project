package com.tradeagent.evaluation;

import com.tradeagent.market.PriceBar;
import com.tradeagent.portfolio.PortfolioPosition;
import com.tradeagent.portfolio.TradeHistory;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public record EntryScoreInput(
            TradeHistory tradeHistory,
            List<PriceBar> barsAfterEntry
    ) {
    }

    public record ExitScoreInput(
            TradeHistory tradeHistory,
            List<PriceBar> barsAroundExit
    ) {
    }

    public record RiskScoreInput(
            TradeHistory buyTrade,
            TradeHistory sellTrade,
            List<PriceBar> holdingBars
    ) {
    }

    public record DiversificationScoreInput(
            Long userId,
            List<PortfolioPosition> positions
    ) {
    }

    public record SectorFitScoreInput(
            String symbol,
            String sectorCode,
            LocalDate tradeDate,
            BigDecimal sectorScore
    ) {
    }
}
