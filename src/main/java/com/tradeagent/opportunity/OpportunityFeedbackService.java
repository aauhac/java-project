package com.tradeagent.opportunity;

import org.springframework.stereotype.Service;

@Service
public class OpportunityFeedbackService {

    public String buildMissedOpportunityFeedback(MissedOpportunity item) {
        return item.getSymbol() + " was on the watchlist, was not bought, and then rallied within 10 trading days.";
    }

    public String buildAvoidedLossFeedback(AvoidedLoss item) {
        return item.getSymbol() + " was on the watchlist, was not bought, and then declined enough to count as an avoided loss.";
    }

    public String buildTradePatternFeedback(BetterTimingDto item) {
        return switch (item.patternType()) {
            case "SOLD_TOO_EARLY" -> item.symbol() + " kept rising after the sell, so this looks like an early exit.";
            case "HELD_TOO_LONG" -> item.symbol() + " experienced a meaningful drawdown before the exit, suggesting the hold lasted too long.";
            case "BETTER_ENTRY" -> item.symbol() + " offered a better entry soon after the buy date.";
            case "BETTER_EXIT" -> item.symbol() + " had more upside after the exit date.";
            default -> item.symbol() + " shows a timing pattern worth reviewing.";
        };
    }
}
