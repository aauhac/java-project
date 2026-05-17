package com.tradeagent.evaluation;

import com.tradeagent.common.ApiResponse;
import com.tradeagent.evaluation.EvaluationModels.DecisionSummaryDto;
import com.tradeagent.evaluation.EvaluationModels.TradeEvaluationDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final TradeEvaluationService tradeEvaluationService;

    public EvaluationController(TradeEvaluationService tradeEvaluationService) {
        this.tradeEvaluationService = tradeEvaluationService;
    }

    @GetMapping("/trade/{tradeId}")
    public ApiResponse<TradeEvaluationDto> evaluateTrade(@PathVariable Long tradeId) {
        return ApiResponse.ok(tradeEvaluationService.evaluateTrade(tradeId));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<TradeEvaluationDto>> evaluateUserTrades(@PathVariable Long userId) {
        return ApiResponse.ok(tradeEvaluationService.evaluateAllTrades(userId));
    }

    @GetMapping("/user/{userId}/summary")
    public ApiResponse<DecisionSummaryDto> getDecisionSummary(@PathVariable Long userId) {
        return ApiResponse.ok(tradeEvaluationService.getDecisionSummary(userId));
    }
}
