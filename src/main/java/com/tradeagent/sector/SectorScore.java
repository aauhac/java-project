package com.tradeagent.sector;

import com.tradeagent.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "sector_score",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sector_score_code_date", columnNames = {"sector_code", "score_date"})
        },
        indexes = {
                @Index(name = "idx_sector_score_code_date", columnList = "sector_code, score_date")
        }
)
public class SectorScore extends BaseEntity {

    @Column(name = "sector_code", nullable = false, length = 16)
    private String sectorCode;

    @Column(name = "score_date", nullable = false)
    private LocalDate scoreDate;

    @Column(name = "news_volume_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal newsVolumeScore;

    @Column(name = "news_tone_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal newsToneScore;

    @Column(name = "price_momentum_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal priceMomentumScore;

    @Column(name = "volume_spike_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal volumeSpikeScore;

    @Column(name = "breadth_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal breadthScore;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_sector_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal totalSectorScore;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    protected SectorScore() {
    }

    public SectorScore(String sectorCode, LocalDate scoreDate, BigDecimal newsVolumeScore, BigDecimal newsToneScore,
                       BigDecimal priceMomentumScore, BigDecimal volumeSpikeScore, BigDecimal breadthScore,
                       BigDecimal totalSectorScore, String status, LocalDateTime analyzedAt) {
        this.sectorCode = sectorCode;
        this.scoreDate = scoreDate;
        this.newsVolumeScore = newsVolumeScore;
        this.newsToneScore = newsToneScore;
        this.priceMomentumScore = priceMomentumScore;
        this.volumeSpikeScore = volumeSpikeScore;
        this.breadthScore = breadthScore;
        this.totalSectorScore = totalSectorScore;
        this.status = status;
        this.analyzedAt = analyzedAt;
    }

    public void updateScores(BigDecimal newsVolumeScore, BigDecimal newsToneScore, BigDecimal priceMomentumScore,
                             BigDecimal volumeSpikeScore, BigDecimal breadthScore,
                             BigDecimal totalSectorScore, String status, LocalDateTime analyzedAt) {
        this.newsVolumeScore = newsVolumeScore;
        this.newsToneScore = newsToneScore;
        this.priceMomentumScore = priceMomentumScore;
        this.volumeSpikeScore = volumeSpikeScore;
        this.breadthScore = breadthScore;
        this.totalSectorScore = totalSectorScore;
        this.status = status;
        this.analyzedAt = analyzedAt;
    }

    public String getSectorCode() {
        return sectorCode;
    }

    public LocalDate getScoreDate() {
        return scoreDate;
    }

    public BigDecimal getNewsVolumeScore() {
        return newsVolumeScore;
    }

    public BigDecimal getNewsToneScore() {
        return newsToneScore;
    }

    public BigDecimal getPriceMomentumScore() {
        return priceMomentumScore;
    }

    public BigDecimal getVolumeSpikeScore() {
        return volumeSpikeScore;
    }

    public BigDecimal getBreadthScore() {
        return breadthScore;
    }

    public BigDecimal getTotalSectorScore() {
        return totalSectorScore;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }
}
