package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.SectorRecordGroup;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SectorGkgAggregator {

    private final SectorRecordRanker sectorRecordRanker;

    public SectorGkgAggregator(SectorRecordRanker sectorRecordRanker) {
        this.sectorRecordRanker = sectorRecordRanker;
    }

    public List<SectorRecordGroup> aggregate(Map<String, List<GdeltGkgRecord>> classifiedRecords) {
        return classifiedRecords.entrySet().stream()
                .map(entry -> buildGroup(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(SectorRecordGroup::sectorCode))
                .toList();
    }

    private SectorRecordGroup buildGroup(String sectorCode, List<GdeltGkgRecord> records) {
        int articleCount = records.size();
        BigDecimal avgTone = records.isEmpty()
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : records.stream()
                .map(record -> record.tone() == null ? BigDecimal.ZERO : record.tone())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(articleCount), 4, RoundingMode.HALF_UP);
        BigDecimal toneScore = avgTone.add(BigDecimal.TEN)
                .divide(BigDecimal.valueOf(20), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .max(BigDecimal.ZERO)
                .min(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal keywordStrengthScore = computeKeywordStrengthScore(sectorCode, records);
        List<GdeltGkgRecord> sampleRecords = sectorRecordRanker.selectTopRecords(sectorCode, records, 20);

        return new SectorRecordGroup(
                sectorCode,
                records,
                articleCount,
                avgTone,
                toneScore,
                keywordStrengthScore,
                topThemes(records),
                topOrganizations(records),
                sampleRecords
        );
    }

    private BigDecimal computeKeywordStrengthScore(String sectorCode, List<GdeltGkgRecord> records) {
        if (records.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        double averageScore = records.stream()
                .mapToInt(record -> sectorRecordRanker.scoreRecord(sectorCode, record))
                .average()
                .orElse(0.0d);
        // scoreRecord upper-bound is effectively around 60 in current weights.
        BigDecimal normalized = BigDecimal.valueOf(averageScore)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (normalized.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        return normalized.setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> topThemes(List<GdeltGkgRecord> records) {
        return topValues(records, record ->
                (record.themes() == null ? "" : record.themes()) + ";" +
                        (record.v2Themes() == null ? "" : record.v2Themes())
        );
    }

    private List<String> topOrganizations(List<GdeltGkgRecord> records) {
        return topValues(records, record ->
                (record.organizations() == null ? "" : record.organizations()) + ";" +
                        (record.v2Organizations() == null ? "" : record.v2Organizations())
        );
    }

    private List<String> topValues(List<GdeltGkgRecord> records, Function<GdeltGkgRecord, String> extractor) {
        Map<String, Long> counts = records.stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .flatMap(value -> List.of(value.split("[;,]")).stream())
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), LinkedHashMap::new, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }
}
