package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.NotFoundException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.sector.SectorApiModels.RefreshNewsResultDto;
import com.tradeagent.sector.SectorApiModels.SectorTrendDto;
import com.tradeagent.sector.gdelt.GdeltRawNewsProvider;
import com.tradeagent.sector.gdelt.GdeltRawProperties;
import com.tradeagent.sector.gdelt.SectorGkgAggregator;
import com.tradeagent.sector.gdelt.SectorRecordClassifier;
import com.tradeagent.sector.gdelt.dto.GdeltRawSample;
import com.tradeagent.sector.gdelt.dto.SectorRecordGroup;
import com.tradeagent.sector.gdelt.dto.SectorSentimentResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SectorGkgTrendService {

    private static final BigDecimal ZERO_2 = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ZERO_4 = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    private static final List<String> SUPPORTED_SECTORS = List.of("SEMI", "AIINF", "EV", "BIO", "CLOUD", "ENERGY");

    private final SectorMasterRepository sectorMasterRepository;
    private final SectorScoreRepository sectorScoreRepository;
    private final GdeltRawProperties rawProperties;
    private final GdeltRawNewsProvider rawNewsProvider;
    private final SectorRecordClassifier sectorRecordClassifier;
    private final SectorGkgAggregator sectorGkgAggregator;
    private final SectorNewsSentimentService sectorNewsSentimentService;

    public SectorGkgTrendService(SectorMasterRepository sectorMasterRepository,
                                 SectorScoreRepository sectorScoreRepository,
                                 GdeltRawProperties rawProperties,
                                 GdeltRawNewsProvider rawNewsProvider,
                                 SectorRecordClassifier sectorRecordClassifier,
                                 SectorGkgAggregator sectorGkgAggregator,
                                 SectorNewsSentimentService sectorNewsSentimentService) {
        this.sectorMasterRepository = sectorMasterRepository;
        this.sectorScoreRepository = sectorScoreRepository;
        this.rawProperties = rawProperties;
        this.rawNewsProvider = rawNewsProvider;
        this.sectorRecordClassifier = sectorRecordClassifier;
        this.sectorGkgAggregator = sectorGkgAggregator;
        this.sectorNewsSentimentService = sectorNewsSentimentService;
    }

    @Transactional
    public RefreshNewsResultDto refreshNews(LocalDate startDate, Integer days, LocalTime sampleTime) {
        ensureMastersExist();
        LocalDate resolvedStartDate = startDate != null ? startDate : DateTimeUtil.today().minusDays(rawProperties.getDefaultDays() - 1L);
        int resolvedDays = days != null && days > 0 ? days : rawProperties.getDefaultDays();
        LocalTime resolvedSampleTime = sampleTime != null
                ? sampleTime
                : LocalTime.of(rawProperties.getDefaultSampleTime() / 100, rawProperties.getDefaultSampleTime() % 100);

        GdeltRawSample sample = rawNewsProvider.fetchMonthlySample(resolvedStartDate, resolvedDays, resolvedSampleTime);
        Map<String, List<com.tradeagent.sector.gdelt.dto.GdeltGkgRecord>> classified = sectorRecordClassifier.classify(sample.records());
        Map<String, List<com.tradeagent.sector.gdelt.dto.GdeltGkgRecord>> normalized = new LinkedHashMap<>();
        for (String sectorCode : SUPPORTED_SECTORS) {
            normalized.put(sectorCode, classified.getOrDefault(sectorCode, List.of()));
        }
        List<SectorRecordGroup> groups = sectorGkgAggregator.aggregate(normalized);
        Map<String, SectorSentimentResult> sentimentBySector = sectorNewsSentimentService.analyzeBySector(groups);
        List<SectorTrendDto> trends = groups.stream()
                .map(group -> upsertScore(group, sentimentBySector.get(group.sectorCode()), sample))
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();

        return new RefreshNewsResultDto(
                sample.startDate(),
                sample.days(),
                sample.sampleTime().format(java.time.format.DateTimeFormatter.ofPattern("HHmm")),
                sample.selectedFileCount(),
                sample.rawRecordCount(),
                trends
        );
    }

    public List<SectorTrendDto> getTrendScores(LocalDate from, LocalDate to) {
        ensureMastersExist();
        LocalDate resolvedTo = to != null ? to : (from != null ? from : DateTimeUtil.today());
        LocalDate resolvedFrom = from != null ? from : resolvedTo;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }

        Map<String, SectorMaster> masterMap = sectorMasterRepository.findAllByOrderBySectorCodeAsc().stream()
                .filter(master -> SUPPORTED_SECTORS.contains(master.getSectorCode()))
                .collect(Collectors.toMap(SectorMaster::getSectorCode, Function.identity()));

        return SUPPORTED_SECTORS.stream()
                .flatMap(code -> sectorScoreRepository.findBySectorCodeAndScoreDateBetweenOrderByScoreDateAsc(code, resolvedFrom, resolvedTo).stream())
                .sorted(Comparator.comparing(SectorScore::getScoreDate).thenComparing(SectorScore::getTotalSectorScore).reversed())
                .map(score -> toDto(masterMap.get(score.getSectorCode()), score))
                .toList();
    }

    public List<SectorTrendDto> getTrendScoresForDate(LocalDate date) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();
        return getTrendScores(resolvedDate, resolvedDate).stream()
                .sorted(Comparator.comparing(SectorTrendDto::totalSectorScore).reversed())
                .toList();
    }

    private SectorTrendDto upsertScore(SectorRecordGroup group, SectorSentimentResult sentiment, GdeltRawSample sample) {
        SectorMaster master = sectorMasterRepository.findBySectorCode(group.sectorCode())
                .orElseThrow(() -> new NotFoundException(ErrorCode.SECTOR_NOT_FOUND, "sector not found for code " + group.sectorCode()));
        BigDecimal newsVolumeScore = calculateNewsVolumeScore(group.articleCount(), sample);
        BigDecimal baseScore = newsVolumeScore.multiply(BigDecimal.valueOf(0.45))
                .add(group.toneScore().multiply(BigDecimal.valueOf(0.35)))
                .add(group.keywordStrengthScore().multiply(BigDecimal.valueOf(0.20)))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalSectorScore = clamp(baseScore.add(sentiment.sentimentAdjustment()));
        String status = statusFor(totalSectorScore);
        LocalDate scoreDate = sample.startDate().plusDays(sample.days() - 1L);
        LocalDateTime analyzedAt = DateTimeUtil.nowUtc();

        SectorScore score = sectorScoreRepository.findBySectorCodeAndScoreDate(group.sectorCode(), scoreDate)
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
                sentiment.reason()
        );
        SectorScore saved = sectorScoreRepository.save(score);
        return toDto(master, saved);
    }

    private BigDecimal calculateNewsVolumeScore(int articleCount, GdeltRawSample sample) {
        int maxPerSector = Math.max(1, Math.min(sample.rawRecordCount(), rawProperties.getMaxRowsPerFile() * Math.max(1, sample.selectedFileCount())));
        return BigDecimal.valueOf(articleCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxPerSector), 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));
    }

    private SectorTrendDto toDto(SectorMaster master, SectorScore score) {
        return new SectorTrendDto(
                score.getSectorCode(),
                master != null ? master.getSectorName() : score.getSectorCode(),
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

    private void ensureMastersExist() {
        List<String> existingCodes = sectorMasterRepository.findAllByOrderBySectorCodeAsc().stream()
                .map(SectorMaster::getSectorCode)
                .toList();
        for (String required : SUPPORTED_SECTORS) {
            if (!existingCodes.contains(required)) {
                throw new ValidationException(ErrorCode.SECTOR_NOT_FOUND, "Missing sector master for code " + required);
            }
        }
    }
}
