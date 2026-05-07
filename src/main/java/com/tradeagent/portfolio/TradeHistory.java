package com.tradeagent.portfolio;

import com.tradeagent.common.BaseEntity;
import com.tradeagent.common.TradeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "trade_history",
        indexes = {
                @Index(name = "idx_trade_history_user_symbol", columnList = "user_id, symbol"),
                @Index(name = "idx_trade_history_traded_at", columnList = "traded_at")
        }
)
public class TradeHistory extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "sector_code", nullable = false, length = 32)
    private String sectorCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 8)
    private TradeType tradeType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "traded_at", nullable = false)
    private LocalDateTime tradedAt;

    protected TradeHistory() {
    }

    public TradeHistory(Long userId, String symbol, String sectorCode, TradeType tradeType,
                        BigDecimal price, Integer quantity, LocalDateTime tradedAt) {
        this.userId = userId;
        this.symbol = symbol;
        this.sectorCode = sectorCode;
        this.tradeType = tradeType;
        this.price = price;
        this.quantity = quantity;
        this.tradedAt = tradedAt;
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

    public TradeType getTradeType() {
        return tradeType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getTradedAt() {
        return tradedAt;
    }
}
