package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.config.GdeltProperties;
import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SectorTrendAnalysisService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal SIMILARITY_THRESHOLD = new BigDecimal("0.12");
    private static final Map<String, List<String>> PROFILE_TERMS_BY_SECTOR = Map.of(
            "SEMI", List.of("semiconductor", "chip", "foundry", "fab", "fabless", "gpu", "memory", "nvidia", "amd", "tsm", "asml"),
            "AIINF", List.of("artificial intelligence", "ai", "infrastructure", "datacenter", "cloud", "compute", "server", "accelerator", "microsoft", "amazon", "google", "oracle"),
            "EV", List.of("electric vehicle", "ev", "battery", "charging", "autonomous", "tesla", "rivian", "nio", "li auto", "byd"),
            "BIO", List.of("biotech", "biopharma", "pharma", "drug", "trial", "fda", "therapy", "vaccine", "moderna", "amgen", "gilead"),
            "CLOUD", List.of("cloud", "cloud software", "saas", "enterprise software", "azure", "oracle cloud", "salesforce", "serviceNow", "msft", "orcl", "crm", "now"),
            "CYBER", List.of("cybersecurity", "cyber", "network security", "endpoint security", "zero trust", "crowdstrike", "palo alto", "zscaler", "crwd", "panw", "zs"),
            "FINPAY", List.of("fintech", "payments", "digital payments", "wallet", "merchant", "card network", "visa", "mastercard", "paypal", "block", "v", "ma", "pypl", "sq")
    );
    private static final List<String> EMBEDDING_VOCABULARY = PROFILE_TERMS_BY_SECTOR.values().stream()
            .flatMap(Collection::stream)
            .flatMap(term -> tokenize(term).stream())
            .distinct()
            .sorted()
            .toList();

    private final SectorMasterRepository sectorMasterRepository;
    private final SectorNewsScoreRepository sectorNewsScoreRepository;
    private final NewsEventRepository newsEventRepository;
    private final GdeltClient gdeltClient;
    private final GdeltProperties gdeltProperties;

    public SectorTrendAnalysisService(SectorMasterRepository sectorMasterRepository,
                                      SectorNewsScoreRepository sectorNewsScoreRepository,
                                      NewsEventRepository newsEventRepository,
                                      GdeltClient gdeltClient,
                                      GdeltProperties gdeltProperties) {
        this.sectorMasterRepository = sectorMasterRepository;
        this.sectorNewsScoreRepository = sectorNewsScoreRepository;
        this.newsEventRepository = newsEventRepository;
        this.gdeltClient = gdeltClient;
        this.gdeltProperties = gdeltProperties;
    }

    @Transactional
    public List<SectorTrendDto> analyzeToday() {
        return analyze(DateTimeUtil.today());
    }

    @Transactional
    public List<SectorTrendDto> analyze(LocalDate date) {
        return analyze(date, false);
    }

    @Transactional
    public List<SectorTrendDto> analyzeStrict(LocalDate date) {
        return analyze(date, true);
    }

    private List<SectorTrendDto> analyze(LocalDate date, boolean failOnFetchError) {
        LocalDate resolvedDate = resolveDate(date);
        LocalDate fromDate = resolvedDate.minusDays(Math.max(gdeltProperties.getLookbackDays(), 0));
        List<SectorMaster> masters = sectorMasterRepository.findAllByOrderBySectorCodeAsc();
        Map<String, SectorMaster> masterMap = masters.stream()
                .collect(Collectors.toMap(SectorMaster::getSectorCode, Function.identity()));
        Map<String, SectorProfile> sectorProfiles = refreshSectorProfiles(masters);

        List<NewsEvent> classifiedNews;
        try {
            classifiedNews = refreshClassifiedNews(fromDate, resolvedDate, sectorProfiles);
        } catch (ExternalApiException ex) {
            if (failOnFetchError) {
                throw new ExternalApiException(ErrorCode.GDELT_API_ERROR, "호출에 실패했습니다. " + ex.getMessage());
            }
            classifiedNews = newsEventRepository.findByPublishedAtBetweenOrderByPublishedAtDesc(
                    fromDate.atStartOfDay(),
                    resolvedDate.atTime(LocalTime.MAX)
            );
        }

        if (classifiedNews.isEmpty()) {
            List<SectorTrendDto> existing = loadExistingScores(masters, resolvedDate);
            if (!existing.isEmpty() && canReuseExistingScores(existing)) {
                return existing;
            }
        }

        Map<String, List<NewsEvent>> newsBySector = classifiedNews.stream()
                .filter(event -> masterMap.containsKey(event.getSectorCode()))
                .collect(Collectors.groupingBy(NewsEvent::getSectorCode));

        List<AnalysisDraft> drafts = masters.stream()
                .map(master -> buildDraft(master.getSectorCode(), newsBySector.getOrDefault(master.getSectorCode(), List.of())))
                .toList();

        List<AnalysisDraft> scoredDrafts = scoreDrafts(drafts);
        Map<String, String> statuses = assignStatuses(scoredDrafts);

        List<SectorTrendDto> trends = new ArrayList<>();
        for (AnalysisDraft draft : scoredDrafts) {
            SectorNewsScore saved = upsert(
                    draft.sectorCode(),
                    resolvedDate,
                    draft.articleCount(),
                    draft.avgToneScore(),
                    draft.newsVolumeScore(),
                    draft.newsToneScore(),
                    draft.keywordStrengthScore(),
                    draft.totalSectorScore(),
                    statuses.getOrDefault(draft.sectorCode(), "NEUTRAL"),
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
        if (scores.size() < masters.size() || hasInconsistentEmptyScores(scores)) {
            return List.of();
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
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();
        return latest.size() == masters.size() ? latest : List.of();
    }

    @Transactional
    public SectorTrendDto getLatestTrendScore(String sectorCode) {
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        SectorMaster master = sectorMasterRepository.findBySectorCode(resolvedSectorCode)
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                        "sector not found for code " + resolvedSectorCode));

        return sectorNewsScoreRepository.findTopBySectorCodeOrderByScoreDateDesc(resolvedSectorCode)
                .map(score -> toDto(master, score))
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND,
                        "sector score not found for code " + resolvedSectorCode));
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

    private List<NewsEvent> refreshClassifiedNews(LocalDate fromDate,
                                                  LocalDate toDate,
                                                  Map<String, SectorProfile> sectorProfiles) {
        List<NewsEvent> fetched = gdeltClient.fetchMarketNews(fromDate, toDate);
        Map<String, NewsEvent> deduped = new LinkedHashMap<>();

        for (NewsEvent event : fetched) {
            ArticleClassification classification = classify(event, sectorProfiles);
            if (classification == null) {
                continue;
            }

            NewsEvent classified = new NewsEvent(
                    classification.sectorCode(),
                    event.getSymbol(),
                    event.getTitle(),
                    event.getSource(),
                    event.getUrl(),
                    event.getToneScore(),
                    event.getPublishedAt(),
                    classification.similarityScore(),
                    classification.embeddingVector()
            );

            String key = dedupeKey(classified);
            NewsEvent existing = deduped.get(key);
            if (existing == null || classified.getSimilarityScore().compareTo(existing.getSimilarityScore()) > 0) {
                deduped.put(key, classified);
            }
        }

        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        newsEventRepository.deleteByPublishedAtBetween(start, end);
        if (deduped.isEmpty()) {
            return List.of();
        }
        return newsEventRepository.saveAll(new ArrayList<>(deduped.values()));
    }

    private ArticleClassification classify(NewsEvent event, Map<String, SectorProfile> sectorProfiles) {
        double[] articleVector = buildEmbeddingVector(event.getTitle());
        if (isZeroVector(articleVector)) {
            return classifyByKeywordFallback(event, sectorProfiles);
        }

        String bestSector = null;
        double bestScore = -1;
        for (SectorProfile profile : sectorProfiles.values()) {
            double similarity = cosineSimilarity(articleVector, profile.vector());
            if (similarity > bestScore) {
                bestScore = similarity;
                bestSector = profile.sectorCode();
            }
        }

        BigDecimal similarityScore = BigDecimal.valueOf(bestScore).setScale(4, RoundingMode.HALF_UP);
        if (bestSector == null || similarityScore.compareTo(SIMILARITY_THRESHOLD) < 0) {
            return classifyByKeywordFallback(event, sectorProfiles);
        }

        return new ArticleClassification(bestSector, similarityScore, serializeVector(articleVector));
    }

    private Map<String, SectorProfile> refreshSectorProfiles(List<SectorMaster> masters) {
        List<SectorMaster> changed = new ArrayList<>();
        Map<String, SectorProfile> profiles = new HashMap<>();

        for (SectorMaster master : masters) {
            String profileText = buildProfileText(master);
            String embeddingVector = serializeVector(buildEmbeddingVector(profileText));
            if (!profileText.equals(master.getProfileText()) || !embeddingVector.equals(master.getEmbeddingVector())) {
                master.updateEmbedding(profileText, embeddingVector);
                changed.add(master);
            }
            profiles.put(master.getSectorCode(), new SectorProfile(master.getSectorCode(), profileText, deserializeVector(embeddingVector)));
        }

        if (!changed.isEmpty()) {
            sectorMasterRepository.saveAll(changed);
        }
        return profiles;
    }

    private String buildProfileText(SectorMaster master) {
        List<String> terms = PROFILE_TERMS_BY_SECTOR.getOrDefault(master.getSectorCode(), List.of(master.getSectorCode().toLowerCase(Locale.ROOT)));
        return (master.getSectorName() + " " + master.getDescription() + " " + String.join(" ", terms) + " " + String.join(" ", terms))
                .toLowerCase(Locale.ROOT);
    }

    private AnalysisDraft buildDraft(String sectorCode, List<NewsEvent> events) {
        int articleCount = events.size();
        if (articleCount == 0) {
            return new AnalysisDraft(
                    sectorCode,
                    0,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    LocalDateTime.now()
            );
        }
        BigDecimal avgToneScore = averageTone(events);
        BigDecimal newsToneScore = calculateNewsToneScore(avgToneScore);
        BigDecimal relevanceScore = averageSimilarity(events);
        LocalDateTime analyzedAt = events.stream()
                .map(NewsEvent::getPublishedAt)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        return new AnalysisDraft(
                sectorCode,
                articleCount,
                avgToneScore,
                ZERO,
                newsToneScore,
                relevanceScore,
                ZERO,
                analyzedAt
        );
    }

    private List<AnalysisDraft> scoreDrafts(List<AnalysisDraft> drafts) {
        int maxArticleCount = drafts.stream()
                .mapToInt(AnalysisDraft::articleCount)
                .max()
                .orElse(0);

        return drafts.stream()
                .map(draft -> {
                    BigDecimal volumeScore = calculateNewsVolumeScore(draft.articleCount(), maxArticleCount);
                    BigDecimal totalScore = calculateTotalScore(volumeScore, draft.newsToneScore(), draft.keywordStrengthScore());
                    return draft.withScores(volumeScore, totalScore);
                })
                .sorted(Comparator.comparing(AnalysisDraft::totalSectorScore).reversed())
                .toList();
    }

    private List<SectorTrendDto> loadExistingScores(List<SectorMaster> masters, LocalDate scoreDate) {
        List<SectorTrendDto> existing = masters.stream()
                .map(master -> sectorNewsScoreRepository.findBySectorCodeAndScoreDate(master.getSectorCode(), scoreDate)
                        .map(score -> toDto(master, score))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();
        return existing.size() == masters.size() ? existing : List.of();
    }

    private boolean canReuseExistingScores(List<SectorTrendDto> scores) {
        return scores.stream().allMatch(score ->
                score.articleCount() > 0 || score.totalSectorScore().compareTo(ZERO) == 0
        );
    }

    private boolean hasInconsistentEmptyScores(List<SectorNewsScore> scores) {
        return scores.stream().anyMatch(score ->
                score.getArticleCount() == 0 && (
                        score.getNewsVolumeScore().compareTo(ZERO) != 0
                                || score.getNewsToneScore().compareTo(ZERO) != 0
                                || score.getKeywordStrengthScore().compareTo(ZERO) != 0
                                || score.getTotalSectorScore().compareTo(ZERO) != 0
                )
        );
    }

    private Map<String, String> assignStatuses(List<AnalysisDraft> drafts) {
        if (drafts.isEmpty()) {
            return Map.of();
        }
        if (drafts.stream().map(AnalysisDraft::totalSectorScore).distinct().count() <= 1) {
            return drafts.stream().collect(Collectors.toMap(AnalysisDraft::sectorCode, ignored -> "NEUTRAL"));
        }

        Map<String, String> statuses = new HashMap<>();
        for (int index = 0; index < Math.min(2, drafts.size()); index++) {
            statuses.put(drafts.get(index).sectorCode(), "STRONG");
        }
        for (int index = Math.max(drafts.size() - 2, 0); index < drafts.size(); index++) {
            statuses.putIfAbsent(drafts.get(index).sectorCode(), "WEAK");
        }
        drafts.forEach(draft -> statuses.putIfAbsent(draft.sectorCode(), "NEUTRAL"));
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

    private BigDecimal averageSimilarity(List<NewsEvent> events) {
        if (events.isEmpty()) {
            return ZERO;
        }
        return events.stream()
                .map(NewsEvent::getSimilarityScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(events.size()), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
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
        BigDecimal score = BigDecimal.valueOf(50).add(avgToneScore.multiply(BigDecimal.valueOf(5)));
        if (score.compareTo(HUNDRED) > 0) {
            return HUNDRED.setScale(2, RoundingMode.HALF_UP);
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalScore(BigDecimal newsVolumeScore, BigDecimal newsToneScore, BigDecimal relevanceScore) {
        return newsVolumeScore.multiply(BigDecimal.valueOf(0.40))
                .add(newsToneScore.multiply(BigDecimal.valueOf(0.35)))
                .add(relevanceScore.multiply(BigDecimal.valueOf(0.25)))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ArticleClassification classifyByKeywordFallback(NewsEvent event, Map<String, SectorProfile> sectorProfiles) {
        String normalizedTitle = event.getTitle() == null ? "" : event.getTitle().toLowerCase(Locale.ROOT);
        String bestSector = null;
        int bestHits = 0;

        for (SectorProfile profile : sectorProfiles.values()) {
            int hits = countProfileTermHits(normalizedTitle, profile.sectorCode());
            if (hits > bestHits) {
                bestHits = hits;
                bestSector = profile.sectorCode();
            }
        }

        if (bestSector == null || bestHits <= 0) {
            return null;
        }

        BigDecimal fallbackScore = BigDecimal.valueOf(Math.min(0.99d, 0.20d + (bestHits * 0.12d)))
                .setScale(4, RoundingMode.HALF_UP);
        return new ArticleClassification(bestSector, fallbackScore, "");
    }

    private int countProfileTermHits(String normalizedTitle, String sectorCode) {
        return PROFILE_TERMS_BY_SECTOR.getOrDefault(sectorCode, List.of()).stream()
                .map(String::toLowerCase)
                .mapToInt(term -> normalizedTitle.contains(term) ? 1 : 0)
                .sum();
    }

    private String dedupeKey(NewsEvent event) {
        if (event.getUrl() != null && !event.getUrl().isBlank()) {
            return event.getUrl().trim().toLowerCase(Locale.ROOT);
        }
        return (event.getTitle() + "|" + event.getSource() + "|" + event.getPublishedAt()).toLowerCase(Locale.ROOT);
    }

    private double[] buildEmbeddingVector(String text) {
        double[] vector = new double[EMBEDDING_VOCABULARY.size()];
        if (text == null || text.isBlank()) {
            return vector;
        }

        Map<String, Long> frequencies = tokenize(text).stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        for (int index = 0; index < EMBEDDING_VOCABULARY.size(); index++) {
            vector[index] = frequencies.getOrDefault(EMBEDDING_VOCABULARY.get(index), 0L);
        }

        double magnitude = Math.sqrt(java.util.Arrays.stream(vector).map(value -> value * value).sum());
        if (magnitude == 0) {
            return vector;
        }
        for (int index = 0; index < vector.length; index++) {
            vector[index] = vector[index] / magnitude;
        }
        return vector;
    }

    private static List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> !token.isBlank())
                .toList();
    }

    private boolean isZeroVector(double[] vector) {
        return java.util.Arrays.stream(vector).allMatch(value -> value == 0);
    }

    private double cosineSimilarity(double[] left, double[] right) {
        double total = 0;
        for (int index = 0; index < left.length; index++) {
            total += left[index] * right[index];
        }
        return total;
    }

    private String serializeVector(double[] vector) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.4f", vector[index]));
        }
        return builder.toString();
    }

    private double[] deserializeVector(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return new double[EMBEDDING_VOCABULARY.size()];
        }

        String[] parts = serialized.split(",");
        double[] vector = new double[EMBEDDING_VOCABULARY.size()];
        for (int index = 0; index < Math.min(parts.length, vector.length); index++) {
            vector[index] = Double.parseDouble(parts[index]);
        }
        return vector;
    }

    private record SectorProfile(String sectorCode, String profileText, double[] vector) {
    }

    private record ArticleClassification(String sectorCode, BigDecimal similarityScore, String embeddingVector) {
    }

    private record AnalysisDraft(
            String sectorCode,
            Integer articleCount,
            BigDecimal avgToneScore,
            BigDecimal newsVolumeScore,
            BigDecimal newsToneScore,
            BigDecimal keywordStrengthScore,
            BigDecimal totalSectorScore,
            LocalDateTime analyzedAt
    ) {
        private AnalysisDraft withScores(BigDecimal newsVolumeScore, BigDecimal totalSectorScore) {
            return new AnalysisDraft(
                    sectorCode,
                    articleCount,
                    avgToneScore,
                    newsVolumeScore,
                    newsToneScore,
                    keywordStrengthScore,
                    totalSectorScore,
                    analyzedAt
            );
        }
    }
}
