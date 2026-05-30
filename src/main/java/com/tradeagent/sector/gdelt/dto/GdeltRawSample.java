package com.tradeagent.sector.gdelt.dto;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record GdeltRawSample(
        LocalDate startDate,
        int days,
        LocalTime sampleTime,
        int selectedFileCount,
        int rawRecordCount,
        List<Path> files,
        List<GdeltGkgRecord> records
) {
}
