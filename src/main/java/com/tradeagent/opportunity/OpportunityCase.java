package com.tradeagent.opportunity;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class OpportunityCase extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "sector_code", nullable = false, length = 32)
    private String sectorCode;

    @Column(name = "change_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal changeRate;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    protected OpportunityCase() {
    }

    protected OpportunityCase(Long userId, String symbol, String sectorCode, BigDecimal changeRate,
                              String reason, LocalDateTime detectedAt) {
        this.userId = userId;
        this.symbol = symbol;
        this.sectorCode = sectorCode;
        this.changeRate = changeRate;
        this.reason = reason;
        this.detectedAt = detectedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public BigDecimal getChangeRate() {
        return changeRate;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
}
