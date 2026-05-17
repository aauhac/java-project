package com.tradeagent.feedback;

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

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private static final String TEMPLATE = "TEMPLATE";
    private static final String TRADE = "TRADE";
    private static final String OPPORTUNITY = "OPPORTUNITY";
    private static final String SECTOR = "SECTOR";
    private static final String OVERALL = "OVERALL";

    private final TradeEvaluationService tradeEvaluationService;
    private final OpportunityAnalysisService opportunityAnalysisService;
    private final PortfolioSectorDiagnosticService portfolioSectorDiagnosticService;
    private final FeedbackPromptBuilder feedbackPromptBuilder;
    private final TemplateFeedbackProvider templateFeedbackProvider;
    private final VllmFeedbackProvider vllmFeedbackProvider;
    private final FeedbackLogRepository feedbackLogRepository;

    public FeedbackService(TradeEvaluationService tradeEvaluationService,
                           OpportunityAnalysisService opportunityAnalysisService,
                           PortfolioSectorDiagnosticService portfolioSectorDiagnosticService,
                           FeedbackPromptBuilder feedbackPromptBuilder,
                           TemplateFeedbackProvider templateFeedbackProvider,
                           VllmFeedbackProvider vllmFeedbackProvider,
                           FeedbackLogRepository feedbackLogRepository) {
        this.tradeEvaluationService = tradeEvaluationService;
        this.opportunityAnalysisService = opportunityAnalysisService;
        this.portfolioSectorDiagnosticService = portfolioSectorDiagnosticService;
        this.feedbackPromptBuilder = feedbackPromptBuilder;
        this.templateFeedbackProvider = templateFeedbackProvider;
        this.vllmFeedbackProvider = vllmFeedbackProvider;
        this.feedbackLogRepository = feedbackLogRepository;
    }

    @Transactional
    public FeedbackResponseDto generateTradeFeedback(Long userId) {
        validateUserId(userId);
        DecisionSummaryDto decisionSummary = tradeEvaluationService.getDecisionSummary(userId);
        String prompt = feedbackPromptBuilder.buildTradeEvaluationPrompt(decisionSummary);
        return generateAndLog(userId, TRADE, TEMPLATE, prompt);
    }

    @Transactional
    public FeedbackResponseDto generateOpportunityFeedback(Long userId) {
        validateUserId(userId);
        OpportunitySummaryDto opportunitySummary = opportunityAnalysisService.getOpportunitySummary(userId);
        String prompt = feedbackPromptBuilder.buildOpportunityPrompt(opportunitySummary);
        return generateAndLog(userId, OPPORTUNITY, TEMPLATE, prompt);
    }

    @Transactional
    public FeedbackResponseDto generateSectorFeedback(Long userId) {
        validateUserId(userId);
        PortfolioSectorDiagnosticDto diagnostic = portfolioSectorDiagnosticService.diagnose(userId);
        String prompt = feedbackPromptBuilder.buildSectorPrompt(diagnostic);
        return generateAndLog(userId, SECTOR, TEMPLATE, prompt);
    }

    @Transactional
    public FeedbackResponseDto generateOverallFeedback(Long userId) {
        validateUserId(userId);
        DecisionSummaryDto decisionSummary = tradeEvaluationService.getDecisionSummary(userId);
        OpportunitySummaryDto opportunitySummary = opportunityAnalysisService.getOpportunitySummary(userId);
        PortfolioSectorDiagnosticDto diagnostic = portfolioSectorDiagnosticService.diagnose(userId);
        String prompt = feedbackPromptBuilder.buildOverallPrompt(decisionSummary, opportunitySummary, diagnostic);
        return generateAndLog(userId, OVERALL, TEMPLATE, prompt);
    }

    private FeedbackResponseDto generateAndLog(Long userId, String feedbackType, String providerType, String prompt) {
        FeedbackProvider provider = resolveProvider(providerType);
        String message = provider.generate(prompt);

        FeedbackLog savedLog = feedbackLogRepository.save(new FeedbackLog(
                userId,
                feedbackType,
                providerType,
                prompt,
                message
        ));

        return new FeedbackResponseDto(
                savedLog.getUserId(),
                savedLog.getFeedbackType(),
                savedLog.getProviderType(),
                savedLog.getResponseText(),
                savedLog.getCreatedAt()
        );
    }

    private FeedbackProvider resolveProvider(String providerType) {
        String resolvedProviderType = providerType == null ? TEMPLATE : providerType.trim().toUpperCase(Locale.ROOT);
        if ("VLLM".equals(resolvedProviderType)) {
            return vllmFeedbackProvider;
        }
        return templateFeedbackProvider;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "userId must be a positive number");
        }
    }
}
