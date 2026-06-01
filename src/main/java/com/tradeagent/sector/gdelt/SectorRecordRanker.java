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
import java.util.regex.Pattern;

@Component
public class SectorRecordRanker {

    private static final int DEFAULT_LIMIT = 20;
    private static final int STRONG_KEYWORD_SCORE = 5;
    private static final int SUPPORT_KEYWORD_SCORE = 2;
    private static final int THEME_BONUS = 8;
    private static final int ORGANIZATION_BONUS = 6;
    private static final int TONE_BONUS_MAX = 12;

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

        int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        return deduped.values().stream()
                .sorted(Comparator
                        .comparingInt((GdeltGkgRecord record) -> scoreRecord(sectorCode, record)).reversed()
                        .thenComparing(record -> absTone(record.tone()), Comparator.reverseOrder()))
                .limit(resolvedLimit)
                .toList();
    }

    public int scoreRecord(String sectorCode, GdeltGkgRecord record) {
        int keywordScore = keywordHits(sectorCode, record);
        int themeBonus = hasStrongTheme(sectorCode, record) ? THEME_BONUS : 0;
        int organizationBonus = hasOrganizationMatch(sectorCode, record) ? ORGANIZATION_BONUS : 0;
        int toneBonus = Math.min(TONE_BONUS_MAX, absTone(record.tone()).multiply(BigDecimal.valueOf(2)).intValue());
        return keywordScore + themeBonus + organizationBonus + toneBonus;
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
            if (containsKeyword(haystack, keyword)) {
                hits += STRONG_KEYWORD_SCORE;
            }
        }
        for (String keyword : keywordProvider.getSupportKeywords(sectorCode)) {
            if (containsKeyword(haystack, keyword)) {
                hits += SUPPORT_KEYWORD_SCORE;
            }
        }
        return hits;
    }

    private boolean hasStrongTheme(String sectorCode, GdeltGkgRecord record) {
        String themes = (safe(record.themes()) + " " + safe(record.v2Themes())).toLowerCase(Locale.ROOT);
        for (String keyword : keywordProvider.getStrongKeywords(sectorCode)) {
            if (containsKeyword(themes, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOrganizationMatch(String sectorCode, GdeltGkgRecord record) {
        String org = (safe(record.organizations()) + " " + safe(record.v2Organizations()) + " " + safe(record.allNames()))
                .toLowerCase(Locale.ROOT);
        for (String keyword : keywordProvider.getStrongKeywords(sectorCode)) {
            if (containsKeyword(org, keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsKeyword(String haystack, String keyword) {
        if (keyword == null) {
            return false;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT).trim();
        if (normalized.length() < 3) {
            return false;
        }
        if (normalized.contains(" ") || normalized.contains("_")) {
            return haystack.contains(normalized);
        }
        return Pattern.compile("\\b" + Pattern.quote(normalized) + "\\b").matcher(haystack).find();
    }

    private BigDecimal absTone(BigDecimal tone) {
        return tone == null ? BigDecimal.ZERO : tone.abs();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
