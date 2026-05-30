package com.tradeagent.sector.gdelt.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GdeltGkgRecord(
        String gkgRecordId,
        LocalDateTime date,
        String sourceName,
        String documentUrl,
        String themes,
        String v2Themes,
        String organizations,
        String v2Organizations,
        String allNames,
        BigDecimal tone,
        String rawTone
) {
}
