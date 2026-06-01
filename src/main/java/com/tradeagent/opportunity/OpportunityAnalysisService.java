package com.tradeagent.opportunity;

import com.tradeagent.opportunity.OpportunityApiModels.BetterTimingDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunityDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunitySummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class OpportunityAnalysisService {

    private final MissedOpportunityDetector missedOpportunityDetector;
    private final AvoidedLossDetector avoidedLossDetector;
    private final TradePatternAnalyzer tradePatternAnalyzer;

    public OpportunityAnalysisService(MissedOpportunityDetector missedOpportunityDetector,
                                      AvoidedLossDetector avoidedLossDetector,
                                      TradePatternAnalyzer tradePatternAnalyzer) {
        this.missedOpportunityDetector = missedOpportunityDetector;
        this.avoidedLossDetector = avoidedLossDetector;
        this.tradePatternAnalyzer = tradePatternAnalyzer;
    }

    public List<OpportunityDto> getTopMissedOpportunities(Long userId) {
        return missedOpportunityDetector.detect(userId).stream()
                .sorted(Comparator.comparing(MissedOpportunity::getExpectedReturn).reversed())
                .limit(10)
                .map(item -> new OpportunityDto(
                        "MISSED_OPPORTUNITY",
                        item.getSymbol(),
                        item.getSectorCode(),
                        item.getExpectedReturn(),
                        item.getReason(),
                        buildMissedOpportunityFeedback(item),
                        item.getDetectedAt()
                ))
                .toList();
    }

    public List<OpportunityDto> getTopAvoidedLosses(Long userId) {
        return avoidedLossDetector.detect(userId).stream()
                .sorted(Comparator.comparing(AvoidedLoss::getAvoidedLossRate).reversed())
                .limit(10)
                .map(item -> new OpportunityDto(
                        "AVOIDED_LOSS",
                        item.getSymbol(),
                        item.getSectorCode(),
                        item.getAvoidedLossRate(),
                        item.getReason(),
                        buildAvoidedLossFeedback(item),
                        item.getDetectedAt()
                ))
                .toList();
    }

    public List<BetterTimingDto> getTradePatterns(Long userId) {
        return java.util.stream.Stream.concat(
                        tradePatternAnalyzer.findSoldTooEarly(userId).stream(),
                        tradePatternAnalyzer.findHeldTooLong(userId).stream())
                .map(item -> new BetterTimingDto(
                        item.patternType(),
                        item.symbol(),
                        item.sectorCode(),
                        item.baseDate(),
                        item.actualPrice(),
                        item.referencePrice(),
                        item.changeRate(),
                        item.reason(),
                        buildTradePatternFeedback(item)
                ))
                .sorted(Comparator.comparing(BetterTimingDto::baseDate).reversed())
                .toList();
    }

    public OpportunitySummaryDto getOpportunitySummary(Long userId) {
        int missedCount = missedOpportunityDetector.detect(userId).size();
        int avoidedCount = avoidedLossDetector.detect(userId).size();
        int soldTooEarlyCount = tradePatternAnalyzer.findSoldTooEarly(userId).size();
        int heldTooLongCount = tradePatternAnalyzer.findHeldTooLong(userId).size();

        return new OpportunitySummaryDto(
                missedCount,
                avoidedCount,
                soldTooEarlyCount,
                heldTooLongCount
        );
    }

    private String buildMissedOpportunityFeedback(MissedOpportunity item) {
        return item.getSymbol() + " was on the watchlist, was not bought, and then rallied within 10 trading days.";
    }

    private String buildAvoidedLossFeedback(AvoidedLoss item) {
        return item.getSymbol() + " was on the watchlist, was not bought, and then declined enough to count as an avoided loss.";
    }

    private String buildTradePatternFeedback(BetterTimingDto item) {
        return switch (item.patternType()) {
            case "SOLD_TOO_EARLY" -> item.symbol() + " kept rising after the sell, so this looks like an early exit.";
            case "HELD_TOO_LONG" -> item.symbol() + " experienced a meaningful drawdown before the exit, suggesting the hold lasted too long.";
            case "BETTER_ENTRY" -> item.symbol() + " offered a better entry soon after the buy date.";
            case "BETTER_EXIT" -> item.symbol() + " had more upside after the exit date.";
            default -> item.symbol() + " shows a timing pattern worth reviewing.";
        };
    }
}
