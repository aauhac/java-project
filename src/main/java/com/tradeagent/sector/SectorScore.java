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

    @Column(name = "article_count", nullable = false)
    private Integer articleCount;

    @Column(name = "avg_tone_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal avgToneScore;

    @Column(name = "keyword_strength_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal keywordStrengthScore;

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

    @Column(name = "source_type", length = 32)
    private String sourceType;

    @Column(name = "sample_file_count")
    private Integer sampleFileCount;

    @Column(name = "raw_record_count")
    private Integer rawRecordCount;

    @Column(name = "top_themes", length = 2000)
    private String topThemes;

    @Column(name = "top_organizations", length = 2000)
    private String topOrganizations;

    @Column(name = "llm_sentiment_reason", length = 4000)
    private String llmSentimentReason;

    protected SectorScore() {
    }

    public SectorScore(String sectorCode, LocalDate scoreDate, BigDecimal newsVolumeScore, BigDecimal newsToneScore,
                       BigDecimal priceMomentumScore, BigDecimal volumeSpikeScore, BigDecimal breadthScore,
                       BigDecimal totalSectorScore, String status, LocalDateTime analyzedAt) {
        this.sectorCode = sectorCode;
        this.scoreDate = scoreDate;
        this.newsVolumeScore = newsVolumeScore;
        this.newsToneScore = newsToneScore;
        this.articleCount = 0;
        this.avgToneScore = BigDecimal.ZERO.setScale(4);
        this.keywordStrengthScore = BigDecimal.ZERO.setScale(2);
        this.priceMomentumScore = priceMomentumScore;
        this.volumeSpikeScore = volumeSpikeScore;
        this.breadthScore = breadthScore;
        this.totalSectorScore = totalSectorScore;
        this.status = status;
        this.analyzedAt = analyzedAt;
        this.sourceType = "LEGACY";
        this.sampleFileCount = 0;
        this.rawRecordCount = 0;
        this.topThemes = "";
        this.topOrganizations = "";
        this.llmSentimentReason = "";
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

    public void updateGkgMetadata(Integer articleCount,
                                  BigDecimal avgToneScore,
                                  BigDecimal keywordStrengthScore,
                                  String sourceType,
                                  Integer sampleFileCount,
                                  Integer rawRecordCount,
                                  String topThemes,
                                  String topOrganizations,
                                  String llmSentimentReason) {
        this.articleCount = articleCount;
        this.avgToneScore = avgToneScore;
        this.keywordStrengthScore = keywordStrengthScore;
        this.sourceType = sourceType;
        this.sampleFileCount = sampleFileCount;
        this.rawRecordCount = rawRecordCount;
        this.topThemes = topThemes;
        this.topOrganizations = topOrganizations;
        this.llmSentimentReason = llmSentimentReason;
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

    public Integer getArticleCount() {
        return articleCount;
    }

    public BigDecimal getAvgToneScore() {
        return avgToneScore;
    }

    public BigDecimal getKeywordStrengthScore() {
        return keywordStrengthScore;
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

    public String getSourceType() {
        return sourceType;
    }

    public Integer getSampleFileCount() {
        return sampleFileCount;
    }

    public Integer getRawRecordCount() {
        return rawRecordCount;
    }

    public String getTopThemes() {
        return topThemes == null ? "" : topThemes;
    }

    public String getTopOrganizations() {
        return topOrganizations == null ? "" : topOrganizations;
    }

    public String getLlmSentimentReason() {
        return llmSentimentReason == null ? "" : llmSentimentReason;
    }
}
