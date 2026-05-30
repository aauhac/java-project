package com.tradeagent.sector.gdelt.dto;

import java.math.BigDecimal;
import java.util.List;

public record SectorRecordGroup(
        String sectorCode,
        List<GdeltGkgRecord> records,
        int articleCount,
        BigDecimal avgTone,
        BigDecimal toneScore,
        BigDecimal keywordStrengthScore,
        List<String> topThemes,
        List<String> topOrganizations,
        List<GdeltGkgRecord> sampleRecords
) {
}
