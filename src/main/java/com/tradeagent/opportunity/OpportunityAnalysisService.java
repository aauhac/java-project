package com.tradeagent.opportunity;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
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
    private final OpportunityFeedbackService opportunityFeedbackService;

    public OpportunityAnalysisService(MissedOpportunityDetector missedOpportunityDetector,
                                      AvoidedLossDetector avoidedLossDetector,
                                      TradePatternAnalyzer tradePatternAnalyzer,
                                      OpportunityFeedbackService opportunityFeedbackService) {
        this.missedOpportunityDetector = missedOpportunityDetector;
        this.avoidedLossDetector = avoidedLossDetector;
        this.tradePatternAnalyzer = tradePatternAnalyzer;
        this.opportunityFeedbackService = opportunityFeedbackService;
    }

    public List<OpportunityDto> getTopMissedOpportunities(Long userId) {
        validateUserId(userId);
        return missedOpportunityDetector.detect(userId).stream()
                .sorted(Comparator.comparing(MissedOpportunity::getExpectedReturn).reversed())
                .limit(10)
                .map(item -> new OpportunityDto(
                        "MISSED_OPPORTUNITY",
                        item.getSymbol(),
                        item.getSectorCode(),
                        item.getExpectedReturn(),
                        item.getReason(),
                        opportunityFeedbackService.buildMissedOpportunityFeedback(item),
                        item.getDetectedAt()
                ))
                .toList();
    }

    public List<OpportunityDto> getTopAvoidedLosses(Long userId) {
        validateUserId(userId);
        return avoidedLossDetector.detect(userId).stream()
                .sorted(Comparator.comparing(AvoidedLoss::getAvoidedLossRate).reversed())
                .limit(10)
                .map(item -> new OpportunityDto(
                        "AVOIDED_LOSS",
                        item.getSymbol(),
                        item.getSectorCode(),
                        item.getAvoidedLossRate(),
                        item.getReason(),
                        opportunityFeedbackService.buildAvoidedLossFeedback(item),
                        item.getDetectedAt()
                ))
                .toList();
    }

    public List<BetterTimingDto> getTradePatterns(Long userId) {
        validateUserId(userId);
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
                        opportunityFeedbackService.buildTradePatternFeedback(item)
                ))
                .sorted(Comparator.comparing(BetterTimingDto::baseDate).reversed())
                .toList();
    }

    public OpportunitySummaryDto getOpportunitySummary(Long userId) {
        validateUserId(userId);
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

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
    }
}
