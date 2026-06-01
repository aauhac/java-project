package com.tradeagent.sector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeagent.config.VllmProperties;
import com.tradeagent.feedback.VllmClient;
import com.tradeagent.sector.gdelt.SectorRecordRanker;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.SectorRecordGroup;
import com.tradeagent.sector.gdelt.dto.SectorSentimentResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SectorNewsSentimentService {

    private static final int TOP_RECORDS_FOR_LLM = 20;
    private static final BigDecimal MIN_ADJUSTMENT = BigDecimal.valueOf(-15);
    private static final BigDecimal MAX_ADJUSTMENT = BigDecimal.valueOf(15);
    private static final String FALLBACK_REASON = "GDELT GKG tone 기반 점수";

    private final VllmClient vllmClient;
    private final VllmProperties vllmProperties;
    private final SectorRecordRanker sectorRecordRanker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SectorNewsSentimentService(VllmClient vllmClient,
                                      VllmProperties vllmProperties,
                                      SectorRecordRanker sectorRecordRanker) {
        this.vllmClient = vllmClient;
        this.vllmProperties = vllmProperties;
        this.sectorRecordRanker = sectorRecordRanker;
    }

    public Map<String, SectorSentimentResult> analyzeBySector(List<SectorRecordGroup> groups) {
        Map<String, SectorSentimentResult> results = new LinkedHashMap<>();
        for (SectorRecordGroup group : groups) {
            results.put(group.sectorCode(), analyzeSingleSector(group));
        }
        return results;
    }

    private SectorSentimentResult analyzeSingleSector(SectorRecordGroup group) {
        if (!vllmProperties.isEnabled() || group.records().isEmpty()) {
            return fallback(group.sectorCode());
        }

        List<GdeltGkgRecord> topRecords = sectorRecordRanker.selectTopRecords(group.sectorCode(), group.records(), TOP_RECORDS_FOR_LLM);
        String prompt = buildPrompt(group, topRecords);
        String response = vllmClient.generateText(prompt);
        return parseResponse(group.sectorCode(), response);
    }

    private String buildPrompt(SectorRecordGroup group, List<GdeltGkgRecord> topRecords) {
        StringBuilder builder = new StringBuilder();
        builder.append("Return strict JSON only.\n");
        builder.append("Fields: sentimentLabel, sentimentAdjustment, reason, positiveFactors, riskFactors\n");
        builder.append("sentimentAdjustment must be a number between -15 and 15.\n");
        builder.append("sectorCode: ").append(group.sectorCode()).append('\n');
        builder.append("articleCount: ").append(group.articleCount()).append('\n');
        builder.append("avgTone: ").append(group.avgTone()).append('\n');
        builder.append("toneScore: ").append(group.toneScore()).append('\n');
        builder.append("keywordStrengthScore: ").append(group.keywordStrengthScore()).append('\n');
        builder.append("topThemes: ").append(group.topThemes()).append('\n');
        builder.append("topOrganizations: ").append(group.topOrganizations()).append('\n');
        builder.append("sampleRecords:\n");
        for (GdeltGkgRecord record : topRecords) {
            builder.append("- titleSource=").append(safe(record.sourceName()))
                    .append(", url=").append(safe(record.documentUrl()))
                    .append(", themes=").append(safe(record.v2Themes()))
                    .append(", orgs=").append(safe(record.v2Organizations()))
                    .append(", tone=").append(record.tone())
                    .append('\n');
        }
        return builder.toString();
    }

    private SectorSentimentResult parseResponse(String sectorCode, String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String label = root.path("sentimentLabel").asText("NEUTRAL");
            BigDecimal adjustment = new BigDecimal(root.path("sentimentAdjustment").asText("0"))
                    .max(MIN_ADJUSTMENT)
                    .min(MAX_ADJUSTMENT)
                    .setScale(2, RoundingMode.HALF_UP);
            String reason = root.path("reason").asText(FALLBACK_REASON);
            List<String> positiveFactors = toStringList(root.path("positiveFactors"));
            List<String> riskFactors = toStringList(root.path("riskFactors"));
            return new SectorSentimentResult(sectorCode, label, adjustment, reason, positiveFactors, riskFactors);
        } catch (Exception ex) {
            return fallback(sectorCode);
        }
    }

    private List<String> toStringList(JsonNode node) {
        List<String> items = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                items.add(item.asText());
            }
        }
        return items;
    }

    private SectorSentimentResult fallback(String sectorCode) {
        return new SectorSentimentResult(
                sectorCode,
                "NEUTRAL",
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                FALLBACK_REASON,
                List.of(),
                List.of()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
