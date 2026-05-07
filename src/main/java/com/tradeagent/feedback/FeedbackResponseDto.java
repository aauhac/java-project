package com.tradeagent.feedback;

import java.time.LocalDateTime;

public record FeedbackResponseDto(
        Long userId,
        String feedbackType,
        String providerType,
        String message,
        LocalDateTime createdAt
) {
}
