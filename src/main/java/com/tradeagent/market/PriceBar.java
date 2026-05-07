package com.tradeagent.market;

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
        name = "price_bars",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_price_bar_symbol_time", columnNames = {"symbol", "bar_time"})
        },
        indexes = {
                @Index(name = "idx_price_bar_symbol_time", columnList = "symbol, bar_time")
        }
)
public class PriceBar extends BaseEntity {

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "bar_time", nullable = false)
    private LocalDateTime barTime;

    @Column(name = "open_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "high_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "low_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "close_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal closePrice;

    @Column(nullable = false)
    private Long volume;

    protected PriceBar() {
    }

    public PriceBar(String symbol, LocalDateTime barTime, BigDecimal openPrice, BigDecimal highPrice,
                    BigDecimal lowPrice, BigDecimal closePrice, Long volume) {
        this.symbol = symbol;
        this.barTime = barTime;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
    }

    public void updateFrom(PriceBar source) {
        this.openPrice = source.openPrice;
        this.highPrice = source.highPrice;
        this.lowPrice = source.lowPrice;
        this.closePrice = source.closePrice;
        this.volume = source.volume;
    }

    public String getSymbol() {
        return symbol;
    }

    public LocalDateTime getBarTime() {
        return barTime;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public Long getVolume() {
        return volume;
    }
}
