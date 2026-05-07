package com.tradeagent.sector;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_event",
        indexes = {
                @Index(name = "idx_news_event_sector_published", columnList = "sector_code, published_at")
        }
)
public class NewsEvent extends BaseEntity {

    @Column(name = "sector_code", nullable = false, length = 16)
    private String sectorCode;

    @Column(length = 16)
    private String symbol;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 128)
    private String source;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(name = "tone_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal toneScore;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    protected NewsEvent() {
    }

    public NewsEvent(String sectorCode, String symbol, String title, String source, String url,
                     BigDecimal toneScore, LocalDateTime publishedAt) {
        this.sectorCode = sectorCode;
        this.symbol = symbol;
        this.title = title;
        this.source = source;
        this.url = url;
        this.toneScore = toneScore;
        this.publishedAt = publishedAt;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTitle() {
        return title;
    }

    public String getSource() {
        return source;
    }

    public String getUrl() {
        return url;
    }

    public BigDecimal getToneScore() {
        return toneScore;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
