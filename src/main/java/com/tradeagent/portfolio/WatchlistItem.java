package com.tradeagent.portfolio;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

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

    private static final String DEFAULT_SECTOR_CODE = "WATCH";

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    private String symbol;

    /*
     * 기존 DB 호환을 위해 내부적으로만 유지한다.
     * 사용자는 sectorCode를 입력하지 않는다.
     */
    @Column(name = "sector_code", nullable = false, length = 32)
    private String sectorCode = DEFAULT_SECTOR_CODE;

    /*
     * 사용자가 선택한 기준일.
     * 기존 DB에 row가 있어도 깨지지 않게 nullable=true로 둔다.
     */
    @Column(name = "watch_start_date")
    private LocalDate watchStartDate;

    protected WatchlistItem() {
    }

    public WatchlistItem(Long userId, String symbol, LocalDate watchStartDate) {
        this.userId = userId;
        this.symbol = symbol;
        this.sectorCode = DEFAULT_SECTOR_CODE;
        this.watchStartDate = watchStartDate;
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

    public LocalDate getWatchStartDate() {
        return watchStartDate;
    }

    public void updateWatchStartDate(LocalDate watchStartDate) {
        this.watchStartDate = watchStartDate;
    }
}