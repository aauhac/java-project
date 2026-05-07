package com.tradeagent.portfolio;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "watchlist_items",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_watchlist_user_symbol", columnNames = {"user_id", "symbol"})
        },
        indexes = {
                @Index(name = "idx_watchlist_user_symbol", columnList = "user_id, symbol")
        }
)
public class WatchlistItem extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(name = "sector_code", nullable = false, length = 32)
    private String sectorCode;

    protected WatchlistItem() {
    }

    public WatchlistItem(Long userId, String symbol, String sectorCode) {
        this.userId = userId;
        this.symbol = symbol;
        this.sectorCode = sectorCode;
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

    public void setSectorCode(String sectorCode) {
        this.sectorCode = sectorCode;
    }
}
