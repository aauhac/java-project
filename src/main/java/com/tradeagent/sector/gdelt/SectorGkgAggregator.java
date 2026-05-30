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

    public List<SectorRecordGroup> aggregate(Map<String, List<GdeltGkgRecord>> classifiedRecords) {
        int maxArticleCount = classifiedRecords.values().stream().mapToInt(List::size).max().orElse(0);
        return classifiedRecords.entrySet().stream()
                .map(entry -> buildGroup(entry.getKey(), entry.getValue(), maxArticleCount))
                .sorted(Comparator.comparing(SectorRecordGroup::sectorCode))
                .toList();
    }

    private SectorRecordGroup buildGroup(String sectorCode, List<GdeltGkgRecord> records, int maxArticleCount) {
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
        BigDecimal keywordStrengthScore = maxArticleCount <= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(articleCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(maxArticleCount), 2, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(100));

        return new SectorRecordGroup(
                sectorCode,
                records,
                articleCount,
                avgTone,
                toneScore,
                keywordStrengthScore,
                topValues(records, GdeltGkgRecord::v2Themes),
                topValues(records, GdeltGkgRecord::v2Organizations),
                records.stream().limit(5).toList()
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
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
    }
}
