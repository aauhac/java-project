package com.tradeagent.opportunity;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.opportunity.OpportunityApiModels.BetterTimingDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunityDto;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunitySummaryDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

    private final OpportunityAnalysisService opportunityAnalysisService;

    public OpportunityController(OpportunityAnalysisService opportunityAnalysisService) {
        this.opportunityAnalysisService = opportunityAnalysisService;
    }

    @GetMapping("/{userId}/missed")
    public ApiResponse<List<OpportunityDto>> getMissed(@PathVariable Long userId) {
        return ApiResponse.ok(opportunityAnalysisService.getTopMissedOpportunities(userId));
    }

    @GetMapping("/{userId}/avoided")
    public ApiResponse<List<OpportunityDto>> getAvoided(@PathVariable Long userId) {
        return ApiResponse.ok(opportunityAnalysisService.getTopAvoidedLosses(userId));
    }

    @GetMapping("/{userId}/patterns")
    public ApiResponse<List<BetterTimingDto>> getPatterns(@PathVariable Long userId) {
        return ApiResponse.ok(opportunityAnalysisService.getTradePatterns(userId));
    }

    @GetMapping("/{userId}/summary")
    public ApiResponse<OpportunitySummaryDto> getSummary(@PathVariable Long userId) {
        return ApiResponse.ok(opportunityAnalysisService.getOpportunitySummary(userId));
    }
}
