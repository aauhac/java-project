package com.tradeagent.sector.gdelt.dto;

import java.math.BigDecimal;
import java.util.List;

public record SectorSentimentResult(
        String sectorCode,
        String sentimentLabel,
        BigDecimal llmScore,
        String reason,
        List<String> positiveFactors,
        List<String> riskFactors
) {
}