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
    private static final BigDecimal DEFAULT_LLM_SCORE = BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
    private static final String FALLBACK_REASON = "vLLM 분석을 사용할 수 없어 중립 점수로 처리했습니다.";

    private final VllmClient vllmClient;
    private final VllmProperties vllmProperties;
    private final SectorRecordRanker sectorRecordRanker;
    private final SectorRefreshProgress progress;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SectorNewsSentimentService(VllmClient vllmClient,
                                      VllmProperties vllmProperties,
                                      SectorRecordRanker sectorRecordRanker,
                                      SectorRefreshProgress progress) {
        this.vllmClient = vllmClient;
        this.vllmProperties = vllmProperties;
        this.sectorRecordRanker = sectorRecordRanker;
        this.progress = progress;
    }

    public Map<String, SectorSentimentResult> analyzeBySector(List<SectorRecordGroup> groups) {
        Map<String, SectorSentimentResult> results = new LinkedHashMap<>();

        int index = 0;
        int baseStep = 32; // 1~30 다운로드, 31 분류, 32 집계 이후 vLLM 시작

        for (SectorRecordGroup group : groups) {
            index++;
            progress.update(
                    "LLM_ANALYSIS",
                    baseStep + index,
                    "vLLM 섹터 감정 분석 중: " + group.sectorCode() + " (" + index + "/" + groups.size() + ")"
            );

            results.put(group.sectorCode(), analyzeSingleSector(group));
        }

        return results;
    }

    private SectorSentimentResult analyzeSingleSector(SectorRecordGroup group) {
        if (!vllmProperties.isEnabled()) {
            progress.log("LLM_SKIP", group.sectorCode() + " vLLM 비활성화 상태 → 중립 점수 사용");
            return fallback(group.sectorCode());
        }

        if (group.records().isEmpty()) {
            progress.log("LLM_SKIP", group.sectorCode() + " 분석할 뉴스 레코드 없음 → 중립 점수 사용");
            return fallback(group.sectorCode());
        }

        try {
            List<GdeltGkgRecord> topRecords =
                    sectorRecordRanker.selectTopRecords(group.sectorCode(), group.records(), TOP_RECORDS_FOR_LLM);

            progress.log(
                    "LLM_ANALYSIS",
                    group.sectorCode() + " vLLM 입력 레코드 " + topRecords.size() + "건 생성"
            );

            String prompt = buildPrompt(group, topRecords);

            progress.log(
                    "LLM_ANALYSIS",
                    group.sectorCode() + " vLLM 호출 시작"
            );

            String response = vllmClient.generateText(prompt);

            progress.log(
                    "LLM_RAW",
                    group.sectorCode() + " vLLM 원문 응답: " + preview(response)
            );

            SectorSentimentResult result = parseResponse(group.sectorCode(), response);

            progress.log(
                    "LLM_ANALYSIS",
                    group.sectorCode() + " vLLM 분석 완료: "
                            + result.sentimentLabel()
                            + " / " + result.llmScore() + "점"
            );

            return result;
        } catch (Exception ex) {
            progress.log(
                    "LLM_WARN",
                    group.sectorCode() + " vLLM 분석 실패 → 중립 점수 사용 / " + ex.getMessage()
            );

            return fallback(group.sectorCode());
        }
    }
    private String preview(String value) {
        if (value == null) {
            return "null";
        }

        String compact = value.replace("\n", " ").replace("\r", " ").trim();

        if (compact.length() > 300) {
            return compact.substring(0, 300) + "...";
        }

        return compact;
    }
    private String buildPrompt(SectorRecordGroup group, List<GdeltGkgRecord> topRecords) {
        StringBuilder builder = new StringBuilder();

        builder.append("You must answer with exactly one valid JSON object.\n");
        builder.append("Do not include markdown.\n");
        builder.append("Do not include explanations outside JSON.\n");
        builder.append("Do not include <think> tags.\n");
        builder.append("Do not copy the schema. Fill the schema with your own analysis.\n");
        builder.append("The JSON object must use exactly these keys:\n");
        builder.append("sentimentLabel, llmScore, reason, positiveFactors, riskFactors\n");
        builder.append("llmScore must be a number from 0 to 100.\n");
        builder.append("sentimentLabel must be one of STRONG, NEUTRAL, WEAK.\n");
        builder.append("Return example format:\n");
        builder.append("{\"sentimentLabel\":\"NEUTRAL\",\"llmScore\":50,\"reason\":\"...\",\"positiveFactors\":[],\"riskFactors\":[]}\n\n");

        builder.append("sectorCode: ").append(group.sectorCode()).append('\n');
        builder.append("sectorName: ").append(SectorConstants.nameOf(group.sectorCode())).append('\n');
        builder.append("articleCount: ").append(group.articleCount()).append('\n');
        builder.append("avgTone: ").append(group.avgTone()).append('\n');
        builder.append("toneScore: ").append(group.toneScore()).append('\n');
        builder.append("keywordStrengthScore: ").append(group.keywordStrengthScore()).append('\n');
        builder.append("topThemes: ").append(group.topThemes()).append('\n');
        builder.append("topOrganizations: ").append(group.topOrganizations()).append('\n');

        builder.append("\nmergedNewsText:\n");
        builder.append(buildMergedNewsText(topRecords)).append('\n');

        return builder.toString();
    }

    private String buildMergedNewsText(List<GdeltGkgRecord> records) {
        StringBuilder builder = new StringBuilder();

        for (GdeltGkgRecord record : records) {
            builder.append("[")
                    .append(safe(record.sourceName()))
                    .append("] ")
                    .append("themes=").append(safe(record.v2Themes()))
                    .append("; orgs=").append(safe(record.v2Organizations()))
                    .append("; tone=").append(record.tone())
                    .append("; url=").append(safe(record.documentUrl()))
                    .append(" | ");
        }

        return builder.toString();
    }

    private SectorSentimentResult parseResponse(String sectorCode, String response) {
        try {
            String cleaned = cleanJsonResponse(response);
            JsonNode root = objectMapper.readTree(cleaned);

            String label = root.path("sentimentLabel").asText("NEUTRAL").trim().toUpperCase();
            if (!label.equals("STRONG") && !label.equals("NEUTRAL") && !label.equals("WEAK")) {
                label = "NEUTRAL";
            }

            BigDecimal llmScore = new BigDecimal(root.path("llmScore").asText("50"))
                    .max(BigDecimal.ZERO)
                    .min(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            String reason = root.path("reason").asText(FALLBACK_REASON);
            List<String> positiveFactors = toStringList(root.path("positiveFactors"));
            List<String> riskFactors = toStringList(root.path("riskFactors"));

            return new SectorSentimentResult(
                    sectorCode,
                    label,
                    llmScore,
                    reason,
                    positiveFactors,
                    riskFactors
            );
        } catch (Exception ex) {
            progress.log(
                    "LLM_PARSE_WARN",
                    sectorCode + " vLLM 응답 JSON 파싱 실패 → 중립 점수 사용 / " + ex.getMessage()
            );
            return fallback(sectorCode);
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring("```json".length()).trim();
        }

        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring("```".length()).trim();
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
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
                DEFAULT_LLM_SCORE,
                FALLBACK_REASON,
                List.of(),
                List.of()
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}