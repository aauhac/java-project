package com.tradeagent.feedback;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import com.tradeagent.evaluation.EvaluationModels.DecisionSummaryDto;
import com.tradeagent.evaluation.TradeEvaluationService;
import com.tradeagent.opportunity.OpportunityAnalysisService;
import com.tradeagent.opportunity.OpportunityApiModels.OpportunitySummaryDto;
import com.tradeagent.sector.PortfolioSectorDiagnosticService;
import com.tradeagent.sector.SectorApiModels.PortfolioSectorDiagnosticDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private static final String PROVIDER = "VLLM_DIRECT";
    private static final String TRADE = "TRADE";
    private static final String OPPORTUNITY = "OPPORTUNITY";
    private static final String SECTOR = "SECTOR";
    private static final String OVERALL = "OVERALL";
    private static final String FALLBACK_MESSAGE = "피드백 생성에 실패했습니다. 잠시 후 다시 시도해주세요.";

    private final TradeEvaluationService tradeEvaluationService;
    private final OpportunityAnalysisService opportunityAnalysisService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;
    private final FeedbackPromptBuilder feedbackPromptBuilder;
    private final VllmClient vllmClient;

    public FeedbackService(TradeEvaluationService tradeEvaluationService,
                           OpportunityAnalysisService opportunityAnalysisService,
                           PortfolioSectorDiagnosticService portfolioSectorDiagnosticService,
                           FeedbackPromptBuilder feedbackPromptBuilder,
                           VllmClient vllmClient) {
        this.tradeEvaluationService = tradeEvaluationService;
        this.opportunityAnalysisService = opportunityAnalysisService;
        this.portfolioSectorDiagnosticService = portfolioSectorDiagnosticService;
        this.feedbackPromptBuilder = feedbackPromptBuilder;
        this.vllmClient = vllmClient;
    }

    @Transactional
    public FeedbackResponseDto generateTradeFeedback(Long userId) {
        validateUserId(userId);
        DecisionSummaryDto decisionSummary = tradeEvaluationService.getDecisionSummary(userId);
        String prompt = feedbackPromptBuilder.buildTradeEvaluationPrompt(decisionSummary);
        return generate(userId, TRADE, prompt);
    }

    @Transactional
    public FeedbackResponseDto generateOpportunityFeedback(Long userId) {
        validateUserId(userId);
        OpportunitySummaryDto opportunitySummary = opportunityAnalysisService.getOpportunitySummary(userId);
        String prompt = feedbackPromptBuilder.buildOpportunityPrompt(opportunitySummary);
        return generate(userId, OPPORTUNITY, prompt);
    }

    @Transactional
    public FeedbackResponseDto generateSectorFeedback(Long userId) {
        validateUserId(userId);
        PortfolioSectorDiagnosticDto diagnostic = portfolioSectorDiagnosticService.diagnose(userId);
        String prompt = feedbackPromptBuilder.buildSectorPrompt(diagnostic);
        return generate(userId, SECTOR, prompt);
    }

    @Transactional
    public FeedbackResponseDto generateOverallFeedback(Long userId) {
        validateUserId(userId);
        DecisionSummaryDto decisionSummary = tradeEvaluationService.getDecisionSummary(userId);
        OpportunitySummaryDto opportunitySummary = opportunityAnalysisService.getOpportunitySummary(userId);
        PortfolioSectorDiagnosticDto diagnostic = portfolioSectorDiagnosticService.diagnose(userId);
        String prompt = feedbackPromptBuilder.buildOverallPrompt(decisionSummary, opportunitySummary, diagnostic);
        return generate(userId, OVERALL, prompt);
    }

    private FeedbackResponseDto generate(Long userId, String feedbackType, String prompt) {
        String message = safeGenerate(prompt);
        return new FeedbackResponseDto(
                userId,
                feedbackType,
                PROVIDER,
                message,
                DateTimeUtil.nowUtc()
        );
    }

    private String safeGenerate(String prompt) {
        try {
            String response = vllmClient.generateText(prompt);
            if (response == null || response.isBlank()) {
                return FALLBACK_MESSAGE;
            }
            return response;
        } catch (Exception ex) {
            return FALLBACK_MESSAGE;
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
    }
}
