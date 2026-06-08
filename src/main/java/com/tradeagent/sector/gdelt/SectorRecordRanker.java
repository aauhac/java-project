package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.SectorKeywordProvider;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * GKG 레코드를 섹터 키워드 일치도 기준으로 점수화하고 순위를 매긴다.
 * scoreRecord 최대값은 SectorGkgAggregator.KEYWORD_SCORE_MAX(60)에 맞춰
 * 강한 키워드 최대 40점 + 보조 키워드 최대 20점으로 설계되었다.
 */
@Component
public class SectorRecordRanker {

    private static final int STRONG_KEYWORD_SCORE = 10;
    private static final int STRONG_KEYWORD_CAP = 40;
    private static final int SUPPORT_KEYWORD_SCORE = 5;
    private static final int SUPPORT_KEYWORD_CAP = 20;

    private final SectorKeywordProvider keywordProvider;

    public SectorRecordRanker(SectorKeywordProvider keywordProvider) {
        this.keywordProvider = keywordProvider;
    }

    /**
     * 레코드의 섹터 적합도 점수를 반환한다. (0 ~ 60)
     */
    public int scoreRecord(String sectorCode, GdeltGkgRecord record) {
        String haystack = buildHaystack(record);

        int strongScore = 0;
        for (String keyword : keywordProvider.getStrongKeywords(sectorCode)) {
            if (containsKeyword(haystack, keyword)) {
                strongScore += STRONG_KEYWORD_SCORE;
            }
        }
        strongScore = Math.min(strongScore, STRONG_KEYWORD_CAP);

        int supportScore = 0;
        for (String keyword : keywordProvider.getSupportKeywords(sectorCode)) {
            if (containsKeyword(haystack, keyword)) {
                supportScore += SUPPORT_KEYWORD_SCORE;
            }
        }
        supportScore = Math.min(supportScore, SUPPORT_KEYWORD_CAP);

        return strongScore + supportScore;
    }

    /**
     * 점수 내림차순으로 상위 N개의 레코드를 반환한다.
     */
    public List<GdeltGkgRecord> selectTopRecords(String sectorCode, List<GdeltGkgRecord> records, int limit) {
        return records.stream()
                .sorted(Comparator.comparingInt((GdeltGkgRecord r) -> scoreRecord(sectorCode, r)).reversed())
                .limit(limit)
                .toList();
    }

    private boolean containsKeyword(String haystack, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return false;
        }
        String normalized = keyword.toLowerCase(Locale.ROOT).trim();
        if (normalized.contains(" ") || normalized.contains("_")) {
            return haystack.contains(normalized);
        }
        return haystack.matches(".*\\b" + java.util.regex.Pattern.quote(normalized) + "\\b.*");
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
