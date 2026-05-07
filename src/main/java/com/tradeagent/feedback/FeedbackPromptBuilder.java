package com.tradeagent.feedback;

import com.tradeagent.evaluation.DecisionSummaryDto;
import com.tradeagent.opportunity.OpportunitySummaryDto;
import com.tradeagent.sector.PortfolioSectorDiagnosticDto;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FeedbackPromptBuilder {

    public String buildTradeEvaluationPrompt(DecisionSummaryDto dto) {
        return String.format(Locale.ROOT,
                "TRADE|entry=%.2f|exit=%.2f|risk=%.2f|diversification=%.2f|sectorFit=%.2f|overall=%.2f|weakness=%s",
                safe(dto.averageEntryScore()),
                safe(dto.averageExitScore()),
                safe(dto.averageRiskScore()),
                safe(dto.averageDiversificationScore()),
                safe(dto.averageSectorFitScore()),
                safe(dto.averageTotalScore()),
                sanitize(dto.mainWeakness()));
    }

    public String buildOpportunityPrompt(OpportunitySummaryDto dto) {
        return String.format(Locale.ROOT,
                "OPPORTUNITY|missed=%d|avoided=%d|soldEarly=%d|heldLong=%d",
                dto.missedCount(),
                dto.avoidedCount(),
                dto.soldTooEarlyCount(),
                dto.heldTooLongCount());
    }

    public String buildSectorPrompt(PortfolioSectorDiagnosticDto dto) {
        return String.format(Locale.ROOT,
                "SECTOR|strongExposure=%.2f|weakExposure=%.2f|message=%s",
                safe(dto.strongExposure()),
                safe(dto.weakExposure()),
                sanitize(dto.message()));
    }

    public String buildOverallPrompt(DecisionSummaryDto tradeSummary,
                                     OpportunitySummaryDto opportunitySummary,
                                     PortfolioSectorDiagnosticDto sectorDiagnostic) {
        return String.format(Locale.ROOT,
                "OVERALL|tradeScore=%.2f|missed=%d|avoided=%d|soldEarly=%d|heldLong=%d|strongExposure=%.2f|weakExposure=%.2f",
                safe(tradeSummary.averageTotalScore()),
                opportunitySummary.missedCount(),
                opportunitySummary.avoidedCount(),
                opportunitySummary.soldTooEarlyCount(),
                opportunitySummary.heldTooLongCount(),
                safe(sectorDiagnostic.strongExposure()),
                safe(sectorDiagnostic.weakExposure()));
    }

    private double safe(java.math.BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', ' ').replace('=', ' ').trim();
    }
}
