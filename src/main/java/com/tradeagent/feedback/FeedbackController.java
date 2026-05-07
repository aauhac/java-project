package com.tradeagent.feedback;

import com.tradeagent.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/{userId}/trade")
    public ApiResponse<FeedbackResponseDto> getTradeFeedback(@PathVariable Long userId) {
        return ApiResponse.ok(feedbackService.generateTradeFeedback(userId));
    }

    @GetMapping("/{userId}/opportunity")
    public ApiResponse<FeedbackResponseDto> getOpportunityFeedback(@PathVariable Long userId) {
        return ApiResponse.ok(feedbackService.generateOpportunityFeedback(userId));
    }

    @GetMapping("/{userId}/sector")
    public ApiResponse<FeedbackResponseDto> getSectorFeedback(@PathVariable Long userId) {
        return ApiResponse.ok(feedbackService.generateSectorFeedback(userId));
    }

    @GetMapping("/{userId}/overall")
    public ApiResponse<FeedbackResponseDto> getOverallFeedback(@PathVariable Long userId) {
        return ApiResponse.ok(feedbackService.generateOverallFeedback(userId));
    }
}
