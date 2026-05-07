package com.tradeagent.market;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "latest_quotes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_latest_quote_symbol", columnNames = "symbol")
        },
        indexes = {
                @Index(name = "idx_latest_quote_symbol", columnList = "symbol")
        }
)
public class LatestQuote extends BaseEntity {

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "last_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal lastPrice;

    @Column(name = "change_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal changeRate;

    protected LatestQuote() {
    }

    public LatestQuote(String symbol, BigDecimal lastPrice, BigDecimal changeRate) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.changeRate = changeRate;
    }

    public void updateQuote(BigDecimal lastPrice, BigDecimal changeRate) {
        this.lastPrice = lastPrice;
        this.changeRate = changeRate;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getLastPrice() {
        return lastPrice;
    }

    public BigDecimal getChangeRate() {
        return changeRate;
    }
}
