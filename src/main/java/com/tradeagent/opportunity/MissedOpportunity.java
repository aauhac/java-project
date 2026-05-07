package com.tradeagent.opportunity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "missed_opportunities",
        indexes = {
                @Index(name = "idx_missed_opportunity_user_symbol", columnList = "user_id, symbol")
        }
)
public class MissedOpportunity extends OpportunityCase {

    @Column(name = "expected_return", nullable = false, precision = 8, scale = 2)
    private BigDecimal expectedReturn;

    protected MissedOpportunity() {
    }

    public MissedOpportunity(Long userId, String symbol, String sectorCode, BigDecimal changeRate,
                             BigDecimal expectedReturn, String reason, LocalDateTime detectedAt) {
        super(userId, symbol, sectorCode, changeRate, reason, detectedAt);
        this.expectedReturn = expectedReturn;
    }

    public BigDecimal getExpectedReturn() {
        return expectedReturn;
    }
}
