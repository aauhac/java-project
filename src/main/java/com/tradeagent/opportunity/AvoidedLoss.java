package com.tradeagent.opportunity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "avoided_losses",
        indexes = {
                @Index(name = "idx_avoided_loss_user_symbol", columnList = "user_id, symbol")
        }
)
public class AvoidedLoss extends OpportunityCase {

    @Column(name = "avoided_loss_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal avoidedLossRate;

    protected AvoidedLoss() {
    }

    public AvoidedLoss(Long userId, String symbol, String sectorCode, BigDecimal changeRate,
                       BigDecimal avoidedLossRate, String reason, LocalDateTime detectedAt) {
        super(userId, symbol, sectorCode, changeRate, reason, detectedAt);
        this.avoidedLossRate = avoidedLossRate;
    }

    public BigDecimal getAvoidedLossRate() {
        return avoidedLossRate;
    }
}
