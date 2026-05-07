package com.tradeagent.evaluation;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trade_evaluations",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_trade_evaluation_trade_history", columnNames = "trade_history_id")
        },
        indexes = {
                @Index(name = "idx_trade_evaluation_trade_history", columnList = "trade_history_id")
        }
)
public class TradeEvaluation extends BaseEntity {

    @Column(name = "trade_history_id", nullable = false)
    private Long tradeHistoryId;

    @Column(name = "entry_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal entryScore;

    @Column(name = "exit_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal exitScore;

    @Column(name = "risk_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "diversification_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal diversificationScore;

    @Column(name = "sector_fit_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal sectorFitScore;

    @Column(name = "total_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "evaluated_at", nullable = false)
    private LocalDateTime evaluatedAt;

    protected TradeEvaluation() {
    }

    public TradeEvaluation(Long tradeHistoryId, BigDecimal entryScore, BigDecimal exitScore, BigDecimal riskScore,
                           BigDecimal diversificationScore, BigDecimal sectorFitScore, BigDecimal totalScore,
                           LocalDateTime evaluatedAt) {
        this.tradeHistoryId = tradeHistoryId;
        this.entryScore = entryScore;
        this.exitScore = exitScore;
        this.riskScore = riskScore;
        this.diversificationScore = diversificationScore;
        this.sectorFitScore = sectorFitScore;
        this.totalScore = totalScore;
        this.evaluatedAt = evaluatedAt;
    }

    public void updateScores(BigDecimal entryScore, BigDecimal exitScore, BigDecimal riskScore,
                             BigDecimal diversificationScore, BigDecimal sectorFitScore, BigDecimal totalScore,
                             LocalDateTime evaluatedAt) {
        this.entryScore = entryScore;
        this.exitScore = exitScore;
        this.riskScore = riskScore;
        this.diversificationScore = diversificationScore;
        this.sectorFitScore = sectorFitScore;
        this.totalScore = totalScore;
        this.evaluatedAt = evaluatedAt;
    }

    public Long getTradeHistoryId() {
        return tradeHistoryId;
    }

    public BigDecimal getEntryScore() {
        return entryScore;
    }

    public BigDecimal getExitScore() {
        return exitScore;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public BigDecimal getDiversificationScore() {
        return diversificationScore;
    }

    public BigDecimal getSectorFitScore() {
        return sectorFitScore;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }
}
