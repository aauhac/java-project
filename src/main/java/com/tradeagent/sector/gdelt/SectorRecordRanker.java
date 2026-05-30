package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.SectorKeywordProvider;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SectorRecordRanker {

    private final SectorKeywordProvider keywordProvider;

    public SectorRecordRanker(SectorKeywordProvider keywordProvider) {
        this.keywordProvider = keywordProvider;
    }

    public List<GdeltGkgRecord> selectTopRecords(String sectorCode, List<GdeltGkgRecord> records, int limit) {
        Map<String, GdeltGkgRecord> deduped = new LinkedHashMap<>();
        for (GdeltGkgRecord record : records) {
            String key = record.documentUrl() == null || record.documentUrl().isBlank()
                    ? record.gkgRecordId()
                    : record.documentUrl().toLowerCase(Locale.ROOT);
            deduped.putIfAbsent(key, record);
        }

        return deduped.values().stream()
                .sorted(Comparator
                        .comparingInt((GdeltGkgRecord record) -> keywordHits(sectorCode, record)).reversed()
                        .thenComparing(record -> absTone(record.tone()), Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    private int keywordHits(String sectorCode, GdeltGkgRecord record) {
        String haystack = String.join(" ",
                        safe(record.documentUrl()),
                        safe(record.themes()),
                        safe(record.v2Themes()),
                        safe(record.organizations()),
                        safe(record.v2Organizations()),
                        safe(record.allNames()))
                .toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String keyword : keywordProvider.getStrongKeywords(sectorCode)) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                hits += 2;
            }
        }
        for (String keyword : keywordProvider.getSupportKeywords(sectorCode)) {
            if (keyword.length() > 2 && haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                hits += 1;
            }
        }
        return hits;
    }

    private BigDecimal absTone(BigDecimal tone) {
        return tone == null ? BigDecimal.ZERO : tone.abs();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
