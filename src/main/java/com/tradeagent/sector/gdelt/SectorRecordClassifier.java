package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.SectorKeywordProvider;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class SectorRecordClassifier {

    private final SectorKeywordProvider keywordProvider;

    public SectorRecordClassifier(SectorKeywordProvider keywordProvider) {
        this.keywordProvider = keywordProvider;
    }

    public Map<String, List<GdeltGkgRecord>> classify(List<GdeltGkgRecord> records) {
        Map<String, List<GdeltGkgRecord>> result = new LinkedHashMap<>();
        keywordProvider.getAllStrongKeywords().keySet().forEach(code -> result.put(code, new ArrayList<>()));

        for (GdeltGkgRecord record : records) {
            String haystack = buildHaystack(record);
            for (String sectorCode : result.keySet()) {
                if (matches(sectorCode, haystack)) {
                    result.get(sectorCode).add(record);
                }
            }
        }
        return result;
    }

    private boolean matches(String sectorCode, String haystack) {
        for (String keyword : keywordProvider.getStrongKeywords(sectorCode)) {
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        int supportHits = 0;
        for (String keyword : keywordProvider.getSupportKeywords(sectorCode)) {
            if (keyword.length() <= 2) {
                continue;
            }
            if (haystack.contains(keyword.toLowerCase(Locale.ROOT))) {
                supportHits++;
            }
        }
        return supportHits >= 2;
    }

    private String buildHaystack(GdeltGkgRecord record) {
        return String.join(" ",
                        safe(record.documentUrl()),
                        safe(record.themes()),
                        safe(record.v2Themes()),
                        safe(record.organizations()),
                        safe(record.v2Organizations()),
                        safe(record.allNames()),
                        safe(record.sourceName()))
                .toLowerCase(Locale.ROOT);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
