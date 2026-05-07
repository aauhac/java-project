package com.tradeagent.feedback;

public record FeedbackRequestDto(
        Long userId,
        String feedbackType,
        String providerType
) {
}
