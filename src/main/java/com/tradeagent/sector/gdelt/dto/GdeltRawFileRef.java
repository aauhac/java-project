package com.tradeagent.sector.gdelt.dto;

import java.time.LocalDateTime;

public record GdeltRawFileRef(
        long sizeBytes,
        String hash,
        String url,
        String filename,
        LocalDateTime timestamp
) {
}
