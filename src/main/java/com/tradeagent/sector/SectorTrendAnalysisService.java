package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SectorTrendAnalysisService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Map<String, List<String>> KEYWORDS_BY_SECTOR = Map.of(
            "SEMI", List.of("semiconductor", "chip", "foundry", "gpu", "nvda", "amd", "tsm"),
            "AIINF", List.of("ai", "infrastructure", "datacenter", "cloud", "compute", "msft", "amzn", "googl", "nvda"),
            "EV", List.of("electric vehicle", "ev", "battery", "charging", "tsla", "rivn", "li"),
            "BIO", List.of("biotech", "pharma", "drug", "trial", "fda", "mrna", "amgn", "gild")
    );

    private final SectorMasterRepository sectorMasterRepository;
    private final SectorNewsScoreRepository sectorNewsScoreRepository;
    private final GdeltClient gdeltClient;

    public SectorTrendAnalysisService(SectorMasterRepository sectorMasterRepository,
                                      SectorNewsScoreRepository sectorNewsScoreRepository,
                                      GdeltClient gdeltClient) {
        this.sectorMasterRepository = sectorMasterRepository;
        this.sectorNewsScoreRepository = sectorNewsScoreRepository;
        this.gdeltClient = gdeltClient;
    }

    @Transactional
    public List<SectorTrendDto> analyzeToday() {
        return analyze(DateTimeUtil.today());
    }

    @Transactional
    public List<SectorTrendDto> analyze(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);
        List<SectorMaster> masters = sectorMasterRepository.findAllByOrderBySectorCodeAsc();
        Map<String, SectorMaster> masterMap = masters.stream()
                .collect(Collectors.toMap(SectorMaster::getSectorCode, Function.identity()));

        List<AnalysisDraft> drafts = masters.stream()
                .map(master -> fetchDraft(master, resolvedDate))
                .toList();

        List<AnalysisDraft> freshDrafts = drafts.stream()
                .filter(AnalysisDraft::fresh)
                .toList();
        Map<String, String> rankedStatus = assignStatuses(freshDrafts);

        List<SectorTrendDto> trends = new ArrayList<>();
        for (AnalysisDraft draft : drafts) {
            if (!draft.fresh()) {
                trends.add(toDto(masterMap.get(draft.sectorCode()), draft.existingScore()));
                continue;
            }

            String status = draft.lockedStatus() != null
                    ? draft.lockedStatus()
                    : rankedStatus.getOrDefault(draft.sectorCode(), "NEUTRAL");

            SectorNewsScore saved = upsert(
                    draft.sectorCode(),
                    resolvedDate,
                    draft.articleCount(),
                    draft.avgToneScore(),
                    draft.newsVolumeScore(),
                    draft.newsToneScore(),
                    draft.keywordStrengthScore(),
                    draft.totalSectorScore(),
                    status,
                    draft.analyzedAt()
            );
            trends.add(toDto(masterMap.get(draft.sectorCode()), saved));
        }

        return trends.stream()
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();
    }

    @Transactional
    public List<SectorTrendDto> getTrendScores(LocalDate date) {
        LocalDate resolvedDate = resolveDate(date);
        List<SectorMaster> masters = sectorMasterRepository.findAllByOrderBySectorCodeAsc();
        List<SectorNewsScore> scores = sectorNewsScoreRepository.findByScoreDateOrderByTotalSectorScoreDesc(resolvedDate);
        if (scores.size() < masters.size()) {
            return analyze(resolvedDate);
        }

        Map<String, SectorMaster> masterMap = masters.stream()
                .collect(Collectors.toMap(SectorMaster::getSectorCode, Function.identity()));
        return scores.stream()
                .map(score -> toDto(masterMap.get(score.getSectorCode()), score))
                .toList();
    }

    @Transactional
    public List<SectorTrendDto> getLatestTrendScores() {
        List<SectorMaster> masters = sectorMasterRepository.findAllByOrderBySectorCodeAsc();
        List<SectorTrendDto> latest = masters.stream()
                .map(master -> sectorNewsScoreRepository.findTopBySectorCodeOrderByScoreDateDesc(master.getSectorCode())
                        .map(score -> toDto(master, score))
                        .orElse(null))
                .filter(item -> item != null)
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();
        return latest.size() == masters.size() ? latest : analyzeToday();
    }

    @Transactional
    public SectorTrendDto getLatestTrendScore(String sectorCode) {
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        SectorMaster master = sectorMasterRepository.findBySectorCode(resolvedSectorCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                        "sector not found for code " + resolvedSectorCode));

        return sectorNewsScoreRepository.findTopBySectorCodeOrderByScoreDateDesc(resolvedSectorCode)
                .map(score -> toDto(master, score))
                .orElseGet(() -> analyzeToday().stream()
                        .filter(item -> item.sectorCode().equals(resolvedSectorCode))
                        .findFirst()
                        .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                                "sector score not found for code " + resolvedSectorCode)));
    }

    public List<SectorTrendDto> getSectorTrend(String sectorCode, LocalDate from, LocalDate to) {
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        LocalDate resolvedFrom = from != null ? from : DateTimeUtil.today().minusDays(6);
        LocalDate resolvedTo = to != null ? to : DateTimeUtil.today();
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }

        SectorMaster master = sectorMasterRepository.findBySectorCode(resolvedSectorCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                        "sector not found for code " + resolvedSectorCode));

        return sectorNewsScoreRepository.findBySectorCodeAndScoreDateBetweenOrderByScoreDateAsc(
                        resolvedSectorCode,
                        resolvedFrom,
                        resolvedTo
                ).stream()
                .map(score -> toDto(master, score))
                .toList();
    }

    private AnalysisDraft fetchDraft(SectorMaster master, LocalDate date) {
        try {
            List<NewsEvent> events = gdeltClient.fetchSectorNews(master.getSectorCode(), date);
            return freshDraft(master.getSectorCode(), events);
        } catch (ExternalApiException ex) {
            return sectorNewsScoreRepository.findBySectorCodeAndScoreDate(master.getSectorCode(), date)
                    .map(score -> AnalysisDraft.fromExisting(master.getSectorCode(), score))
                    .orElseGet(() -> AnalysisDraft.zero(master.getSectorCode(), LocalDateTime.now()));
        }
    }

    private AnalysisDraft freshDraft(String sectorCode, List<NewsEvent> events) {
        int articleCount = events.size();
        BigDecimal avgTone = averageTone(events);
        BigDecimal keywordStrength = calculateKeywordStrengthScore(sectorCode, events);
        return new AnalysisDraft(
                sectorCode,
                articleCount,
                avgTone,
                ZERO,
                calculateNewsToneScore(avgTone),
                keywordStrength,
                ZERO,
                null,
                LocalDateTime.now(),
                true,
                null
        );
    }

    private Map<String, String> assignStatuses(List<AnalysisDraft> freshDrafts) {
        if (freshDrafts.isEmpty()) {
            return Map.of();
        }

        int maxArticleCount = freshDrafts.stream()
                .mapToInt(AnalysisDraft::articleCount)
                .max()
                .orElse(0);

        List<AnalysisDraft> scored = freshDrafts.stream()
                .map(draft -> draft.withScores(
                        calculateNewsVolumeScore(draft.articleCount(), maxArticleCount),
                        draft.newsToneScore(),
                        calculateTotalScore(
                                calculateNewsVolumeScore(draft.articleCount(), maxArticleCount),
                                draft.newsToneScore(),
                                draft.keywordStrengthScore()
                        )
                ))
                .sorted(Comparator.comparing(AnalysisDraft::totalSectorScore).reversed())
                .toList();

        if (scored.stream().map(AnalysisDraft::totalSectorScore).distinct().count() <= 1) {
            return scored.stream().collect(Collectors.toMap(AnalysisDraft::sectorCode, ignored -> "NEUTRAL"));
        }

        Map<String, String> statuses = new HashMap<>();
        for (int index = 0; index < Math.min(2, scored.size()); index++) {
            statuses.put(scored.get(index).sectorCode(), "STRONG");
        }
        for (int index = Math.max(scored.size() - 2, 0); index < scored.size(); index++) {
            statuses.putIfAbsent(scored.get(index).sectorCode(), "WEAK");
        }
        scored.forEach(draft -> statuses.putIfAbsent(draft.sectorCode(), "NEUTRAL"));
        freshDrafts.forEach(draft -> draft.applyComputedScores(scored.stream()
                .filter(item -> item.sectorCode().equals(draft.sectorCode()))
                .findFirst()
                .orElse(draft)));
        return statuses;
    }

    private SectorNewsScore upsert(String sectorCode, LocalDate scoreDate, Integer articleCount, BigDecimal avgToneScore,
                                   BigDecimal newsVolumeScore, BigDecimal newsToneScore, BigDecimal keywordStrengthScore,
                                   BigDecimal totalSectorScore, String status, LocalDateTime analyzedAt) {
        return sectorNewsScoreRepository.findBySectorCodeAndScoreDate(sectorCode, scoreDate)
                .map(existing -> {
                    existing.update(
                            articleCount,
                            avgToneScore,
                            newsVolumeScore,
                            newsToneScore,
                            keywordStrengthScore,
                            totalSectorScore,
                            status,
                            analyzedAt
                    );
                    return sectorNewsScoreRepository.save(existing);
                })
                .orElseGet(() -> sectorNewsScoreRepository.save(new SectorNewsScore(
                        sectorCode,
                        scoreDate,
                        articleCount,
                        avgToneScore,
                        newsVolumeScore,
                        newsToneScore,
                        keywordStrengthScore,
                        totalSectorScore,
                        status,
                        analyzedAt
                )));
    }

    private SectorTrendDto toDto(SectorMaster master, SectorNewsScore score) {
        return new SectorTrendDto(
                master.getSectorCode(),
                master.getSectorName(),
                score.getScoreDate(),
                score.getArticleCount(),
                score.getAvgToneScore().setScale(4, RoundingMode.HALF_UP),
                score.getNewsVolumeScore().setScale(2, RoundingMode.HALF_UP),
                score.getNewsToneScore().setScale(2, RoundingMode.HALF_UP),
                score.getKeywordStrengthScore().setScale(2, RoundingMode.HALF_UP),
                score.getTotalSectorScore().setScale(2, RoundingMode.HALF_UP),
                score.getStatus(),
                score.getAnalyzedAt()
        );
    }

    private LocalDate resolveDate(LocalDate date) {
        return date != null ? date : DateTimeUtil.today();
    }

    private String normalizeSectorCode(String sectorCode) {
        if (sectorCode == null || sectorCode.isBlank()) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "sectorCode must not be blank");
        }
        return sectorCode.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal averageTone(List<NewsEvent> events) {
        if (events.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return events.stream()
                .map(NewsEvent::getToneScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(events.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateKeywordStrengthScore(String sectorCode, List<NewsEvent> events) {
        if (events.isEmpty()) {
            return ZERO;
        }

        List<String> keywords = KEYWORDS_BY_SECTOR.getOrDefault(sectorCode, List.of(sectorCode.toLowerCase(Locale.ROOT)));
        int matches = 0;
        int maxMatches = events.size() * keywords.size();
        for (NewsEvent event : events) {
            String content = (event.getTitle() + " " + event.getSource()).toLowerCase(Locale.ROOT);
            for (String keyword : keywords) {
                if (content.contains(keyword.toLowerCase(Locale.ROOT))) {
                    matches++;
                }
            }
        }
        if (maxMatches == 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(matches)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(maxMatches), 2, RoundingMode.HALF_UP)
                .min(HUNDRED);
    }

    private BigDecimal calculateNewsVolumeScore(int articleCount, int maxArticleCount) {
        if (articleCount <= 0 || maxArticleCount <= 0) {
            return ZERO;
        }
        return BigDecimal.valueOf(articleCount)
                .multiply(HUNDRED)
                .divide(BigDecimal.valueOf(maxArticleCount), 2, RoundingMode.HALF_UP)
                .min(HUNDRED);
    }

    private BigDecimal calculateNewsToneScore(BigDecimal avgToneScore) {
        BigDecimal score = BigDecimal.valueOf(50).add(avgToneScore.multiply(BigDecimal.TEN));
        if (score.compareTo(HUNDRED) > 0) {
            return HUNDRED.setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalScore(BigDecimal newsVolumeScore, BigDecimal newsToneScore, BigDecimal keywordStrengthScore) {
        return newsVolumeScore.multiply(BigDecimal.valueOf(0.5))
                .add(newsToneScore.multiply(BigDecimal.valueOf(0.3)))
                .add(keywordStrengthScore.multiply(BigDecimal.valueOf(0.2)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static final class AnalysisDraft {
        private final String sectorCode;
        private final Integer articleCount;
        private final BigDecimal avgToneScore;
        private BigDecimal newsVolumeScore;
        private final BigDecimal newsToneScore;
        private final BigDecimal keywordStrengthScore;
        private BigDecimal totalSectorScore;
        private final String lockedStatus;
        private final LocalDateTime analyzedAt;
        private final boolean fresh;
        private final SectorNewsScore existingScore;

        private AnalysisDraft(String sectorCode, Integer articleCount, BigDecimal avgToneScore,
                              BigDecimal newsVolumeScore, BigDecimal newsToneScore, BigDecimal keywordStrengthScore,
                              BigDecimal totalSectorScore, String lockedStatus, LocalDateTime analyzedAt,
                              boolean fresh, SectorNewsScore existingScore) {
            this.sectorCode = sectorCode;
            this.articleCount = articleCount;
            this.avgToneScore = avgToneScore;
            this.newsVolumeScore = newsVolumeScore;
            this.newsToneScore = newsToneScore;
            this.keywordStrengthScore = keywordStrengthScore;
            this.totalSectorScore = totalSectorScore;
            this.lockedStatus = lockedStatus;
            this.analyzedAt = analyzedAt;
            this.fresh = fresh;
            this.existingScore = existingScore;
        }

        private static AnalysisDraft fromExisting(String sectorCode, SectorNewsScore score) {
            return new AnalysisDraft(
                    sectorCode,
                    score.getArticleCount(),
                    score.getAvgToneScore(),
                    score.getNewsVolumeScore(),
                    score.getNewsToneScore(),
                    score.getKeywordStrengthScore(),
                    score.getTotalSectorScore(),
                    score.getStatus(),
                    score.getAnalyzedAt(),
                    false,
                    score
            );
        }

        private static AnalysisDraft zero(String sectorCode, LocalDateTime analyzedAt) {
            return new AnalysisDraft(
                    sectorCode,
                    0,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    "NEUTRAL",
                    analyzedAt,
                    true,
                    null
            );
        }

        private AnalysisDraft withScores(BigDecimal newsVolumeScore, BigDecimal newsToneScore, BigDecimal totalSectorScore) {
            return new AnalysisDraft(
                    sectorCode,
                    articleCount,
                    avgToneScore,
                    newsVolumeScore,
                    newsToneScore,
                    keywordStrengthScore,
                    totalSectorScore,
                    lockedStatus,
                    analyzedAt,
                    fresh,
                    existingScore
            );
        }

        private void applyComputedScores(AnalysisDraft scored) {
            this.newsVolumeScore = scored.newsVolumeScore;
            this.totalSectorScore = scored.totalSectorScore;
        }

        private String sectorCode() {
            return sectorCode;
        }

        private Integer articleCount() {
            return articleCount;
        }

        private BigDecimal avgToneScore() {
            return avgToneScore;
        }

        private BigDecimal newsVolumeScore() {
            return newsVolumeScore;
        }

        private BigDecimal newsToneScore() {
            return newsToneScore;
        }

        private BigDecimal keywordStrengthScore() {
            return keywordStrengthScore;
        }

        private BigDecimal totalSectorScore() {
            return totalSectorScore;
        }

        private String lockedStatus() {
            return lockedStatus;
        }

        private LocalDateTime analyzedAt() {
            return analyzedAt;
        }

        private boolean fresh() {
            return fresh;
        }

        private SectorNewsScore existingScore() {
            return existingScore;
        }
    }
}
