package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.SectorKeywordProvider;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.SectorRecordGroup;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SectorGkgPhase2Test {

    private final SectorKeywordProvider keywordProvider = new SectorKeywordProvider();
    private final SectorRecordClassifier classifier = new SectorRecordClassifier(keywordProvider);
    private final SectorRecordRanker ranker = new SectorRecordRanker(keywordProvider);
    private final SectorGkgAggregator aggregator = new SectorGkgAggregator(ranker);

    @Test
    void classifyShouldContainAllSixSectors() {
        Map<String, List<GdeltGkgRecord>> result = classifier.classify(List.of(
                record("https://example.com/a", "SEMI", "", "", "", "", "", "nvidia chip foundry", 1),
                record("https://example.com/b", "AIINF", "", "", "", "", "", "machine learning data center", 1)
        ));

        assertTrue(result.containsKey("SEMI"));
        assertTrue(result.containsKey("AIINF"));
        assertTrue(result.containsKey("EV"));
        assertTrue(result.containsKey("BIO"));
        assertTrue(result.containsKey("CLOUD"));
        assertTrue(result.containsKey("ENERGY"));
    }

    @Test
    void energyShouldMatchEnvOilAndOilAndGasTheme() {
        GdeltGkgRecord energyRecord = record(
                "https://example.com/energy",
                "ENV_OIL;OIL_AND_GAS",
                "ENV_OIL;OIL_AND_GAS",
                "",
                "",
                "",
                "",
                "market update",
                3
        );

        Map<String, List<GdeltGkgRecord>> result = classifier.classify(List.of(energyRecord));
        assertFalse(result.get("ENERGY").isEmpty());
    }

    @Test
    void aiSingleTokenShouldNotCauseFalsePositive() {
        GdeltGkgRecord noisy = record(
                "https://example.com/noise",
                "",
                "",
                "",
                "",
                "",
                "",
                "this article mentions ai only",
                0
        );

        Map<String, List<GdeltGkgRecord>> result = classifier.classify(List.of(noisy));
        assertTrue(result.get("AIINF").isEmpty());
    }

    @Test
    void aggregatorShouldBuildStats() {
        GdeltGkgRecord r1 = record("https://example.com/1", "semiconductor", "chip", "NVIDIA", "NVIDIA", "NVIDIA", "NVIDIA", "nvidia chip", 4);
        GdeltGkgRecord r2 = record("https://example.com/2", "semiconductor", "foundry", "TSMC", "TSMC", "TSMC", "TSMC", "foundry", -2);

        Map<String, List<GdeltGkgRecord>> classified = classifier.classify(List.of(r1, r2));
        List<SectorRecordGroup> groups = aggregator.aggregate(classified);

        SectorRecordGroup semi = groups.stream().filter(group -> group.sectorCode().equals("SEMI")).findFirst().orElseThrow();
        assertEquals(2, semi.articleCount());
        assertTrue(semi.toneScore().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(semi.toneScore().compareTo(BigDecimal.valueOf(100)) <= 0);
        assertFalse(semi.sampleRecords().isEmpty());
    }

    private GdeltGkgRecord record(String documentUrl,
                                  String themes,
                                  String v2Themes,
                                  String organizations,
                                  String v2Organizations,
                                  String source,
                                  String rawTone,
                                  String allNames,
                                  double tone) {
        return new GdeltGkgRecord(
                "id-" + documentUrl,
                LocalDateTime.now(),
                source,
                documentUrl,
                themes,
                v2Themes,
                organizations,
                v2Organizations,
                allNames,
                BigDecimal.valueOf(tone),
                rawTone
        );
    }
}
