package com.tradeagent.feedback;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "feedback_log",
        indexes = {
                @Index(name = "idx_feedback_log_user_type", columnList = "user_id, feedback_type")
        }
)
public class FeedbackLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feedback_type", nullable = false, length = 32)
    private String feedbackType;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @Column(name = "request_text", nullable = false, length = 4000)
    private String requestText;

    @Column(name = "response_text", nullable = false, length = 4000)
    private String responseText;

    protected FeedbackLog() {
    }

    public FeedbackLog(Long userId, String feedbackType, String providerType, String requestText, String responseText) {
        this.userId = userId;
        this.feedbackType = feedbackType;
        this.providerType = providerType;
        this.requestText = requestText;
        this.responseText = responseText;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFeedbackType() {
        return feedbackType;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getRequestText() {
        return requestText;
    }

    public String getResponseText() {
        return responseText;
    }
}
