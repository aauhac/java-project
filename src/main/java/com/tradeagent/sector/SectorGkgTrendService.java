package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ValidationException;
import com.tradeagent.sector.SectorApiModels.RefreshNewsResultDto;
import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import com.tradeagent.sector.gdelt.GdeltRawNewsProvider;
import com.tradeagent.sector.gdelt.SectorGkgAggregator;
import com.tradeagent.sector.gdelt.SectorRecordClassifier;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.GdeltRawSample;
import com.tradeagent.sector.gdelt.dto.SectorRecordGroup;
import com.tradeagent.sector.gdelt.dto.SectorSentimentResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SectorGkgTrendService {

    private static final BigDecimal ZERO_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int MAX_ROWS_PER_FILE = 2000;

    // 30 download + 1 classify + 1 aggregate + 6 llm + 6 save = 44
    private static final int TOTAL_PROGRESS_STEPS = 44;

    private final SectorScoreRepository sectorScoreRepository;
    private final GdeltRawNewsProvider rawNewsProvider;
    private final SectorRecordClassifier sectorRecordClassifier;
    private final SectorGkgAggregator sectorGkgAggregator;
    private final SectorNewsSentimentService sectorNewsSentimentService;
    private final SectorRefreshProgress progress;

    public SectorGkgTrendService(SectorScoreRepository sectorScoreRepository,
                                 GdeltRawNewsProvider rawNewsProvider,
                                 SectorRecordClassifier sectorRecordClassifier,
                                 SectorGkgAggregator sectorGkgAggregator,
                                 SectorNewsSentimentService sectorNewsSentimentService,
                                 SectorRefreshProgress progress) {
        this.sectorScoreRepository = sectorScoreRepository;
        this.rawNewsProvider = rawNewsProvider;
        this.sectorRecordClassifier = sectorRecordClassifier;
        this.sectorGkgAggregator = sectorGkgAggregator;
        this.sectorNewsSentimentService = sectorNewsSentimentService;
        this.progress = progress;
    }

    @Transactional
    public RefreshNewsResultDto refreshNews() {
        progress.start(TOTAL_PROGRESS_STEPS, "뉴스 갱신을 시작합니다.");

        try {
            GdeltRawSample sample = rawNewsProvider.fetchMonthlySample();

            progress.log(
                    "DOWNLOAD",
                    "원시 데이터 수집 결과: 파일 " + sample.selectedFileCount()
                            + "개, 전체 레코드 " + sample.rawRecordCount() + "건"
            );

            progress.update("CLASSIFY", 31, "GDELT 레코드를 섹터별로 분류하는 중입니다.");

            Map<String, List<GdeltGkgRecord>> classified =
                    sectorRecordClassifier.classify(sample.records());

            progress.log("CLASSIFY", "섹터 분류 완료");

            Map<String, List<GdeltGkgRecord>> normalized = new LinkedHashMap<>();

            for (String sectorCode : SectorConstants.SUPPORTED_SECTORS) {
                List<GdeltGkgRecord> sectorRecords =
                        classified.getOrDefault(sectorCode, List.of());

                normalized.put(sectorCode, sectorRecords);

                progress.log(
                        "CLASSIFY",
                        sectorCode + " 분류 결과: " + sectorRecords.size() + "건"
                );
            }

            progress.update("AGGREGATE", 32, "섹터별 뉴스 통계를 집계하는 중입니다.");

            List<SectorRecordGroup> groups = sectorGkgAggregator.aggregate(normalized);

            for (SectorRecordGroup group : groups) {
                progress.log(
                        "AGGREGATE",
                        group.sectorCode()
                                + " 집계 완료: 기사 " + group.articleCount()
                                + "건, avgTone=" + group.avgTone()
                                + ", keywordScore=" + group.keywordStrengthScore()
                );
            }

            Map<String, SectorSentimentResult> sentimentBySector =
                    sectorNewsSentimentService.analyzeBySector(groups);

            List<SectorTrendDto> trends = new ArrayList<>();

            int saveBaseStep = 38;
            int saveIndex = 0;

            for (SectorRecordGroup group : groups) {
                saveIndex++;

                progress.update(
                        "SAVE",
                        saveBaseStep + saveIndex,
                        "섹터 점수 저장 중: " + group.sectorCode()
                                + " (" + saveIndex + "/" + groups.size() + ")"
                );

                SectorTrendDto trend = upsertScore(
                        group,
                        sentimentBySector.get(group.sectorCode()),
                        sample
                );

                trends.add(trend);

                progress.log(
                        "SAVE",
                        group.sectorCode()
                                + " 저장 완료: 최종 점수 "
                                + trend.totalSectorScore()
                                + "점 / " + trend.status()
                );
            }

            trends = trends.stream()
                    .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                    .toList();

            progress.finish("뉴스 갱신이 완료되었습니다.");

            return new RefreshNewsResultDto(
                    sample.startDate(),
                    sample.days(),
                    sample.sampleTime().format(java.time.format.DateTimeFormatter.ofPattern("HHmm")),
                    sample.selectedFileCount(),
                    sample.rawRecordCount(),
                    trends
            );
        } catch (Exception ex) {
            progress.fail(ex.getMessage());
            throw ex;
        }
    }

    public List<SectorTrendDto> getTrendScores(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to != null ? to : (from != null ? from : DateTimeUtil.today());
        LocalDate resolvedFrom = from != null ? from : resolvedTo;

        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }

        return SectorConstants.SUPPORTED_SECTORS.stream()
                .flatMap(code -> sectorScoreRepository
                        .findBySectorCodeAndScoreDateBetweenOrderByScoreDateAsc(
                                code,
                                resolvedFrom,
                                resolvedTo
                        )
                        .stream())
                .sorted(Comparator.comparing(SectorScore::getScoreDate)
                        .thenComparing(SectorScore::getTotalSectorScore)
                        .reversed())
                .map(this::toDto)
                .toList();
    }

    public List<SectorTrendDto> getTrendScoresForDate(LocalDate date) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();

        return getTrendScores(resolvedDate, resolvedDate).stream()
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();
    }

    private SectorTrendDto upsertScore(SectorRecordGroup group,
                                       SectorSentimentResult sentiment,
                                       GdeltRawSample sample) {
        SectorSentimentResult safeSentiment = sentiment != null
                ? sentiment
                : new SectorSentimentResult(
                group.sectorCode(),
                "NEUTRAL",
                BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP),
                "vLLM sentiment unavailable",
                List.of(),
                List.of()
        );

        BigDecimal newsVolumeScore = calculateNewsVolumeScore(group.articleCount(), sample);

        BigDecimal ruleScore = newsVolumeScore.multiply(BigDecimal.valueOf(0.30))
                .add(group.toneScore().multiply(BigDecimal.valueOf(0.30)))
                .add(group.keywordStrengthScore().multiply(BigDecimal.valueOf(0.40)))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal llmScore = safeSentiment.llmScore();

        BigDecimal totalSectorScore = clamp(
                ruleScore.multiply(BigDecimal.valueOf(0.40))
                        .add(llmScore.multiply(BigDecimal.valueOf(0.60)))
        );

        String status = statusFor(totalSectorScore);
        LocalDate scoreDate = sample.startDate().plusDays(sample.days() - 1L);
        LocalDateTime analyzedAt = DateTimeUtil.nowUtc();

        SectorScore score = sectorScoreRepository.findBySectorCodeAndScoreDate(
                        group.sectorCode(),
                        scoreDate
                )
                .orElseGet(() -> new SectorScore(
                        group.sectorCode(),
                        scoreDate,
                        newsVolumeScore,
                        group.toneScore(),
                        ZERO_2,
                        ZERO_2,
                        ZERO_2,
                        totalSectorScore,
                        status,
                        analyzedAt
                ));

        score.updateScores(
                newsVolumeScore,
                group.toneScore(),
                ZERO_2,
                ZERO_2,
                ZERO_2,
                totalSectorScore,
                status,
                analyzedAt
        );

        score.updateGkgMetadata(
                group.articleCount(),
                group.avgTone(),
                group.keywordStrengthScore(),
                "GDELT_GKG",
                sample.selectedFileCount(),
                sample.rawRecordCount(),
                String.join(", ", group.topThemes()),
                String.join(", ", group.topOrganizations()),
                safeSentiment.reason()
        );

        SectorScore saved = sectorScoreRepository.save(score);
        return toDto(saved);
    }

    private BigDecimal calculateNewsVolumeScore(int articleCount, GdeltRawSample sample) {
        int maxPerSector = Math.max(
                1,
                Math.min(
                        sample.rawRecordCount(),
                        MAX_ROWS_PER_FILE * Math.max(1, sample.selectedFileCount())
                )
        );

        return BigDecimal.valueOf(articleCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxPerSector), 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    private SectorTrendDto toDto(SectorScore score) {
        return new SectorTrendDto(
                score.getSectorCode(),
                SectorConstants.nameOf(score.getSectorCode()),
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

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return ZERO_2;
        }

        if (value.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }

        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String statusFor(BigDecimal totalScore) {
        if (totalScore.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "STRONG";
        }

        if (totalScore.compareTo(BigDecimal.valueOf(40)) <= 0) {
            return "WEAK";
        }

        return "NEUTRAL";
    }
}