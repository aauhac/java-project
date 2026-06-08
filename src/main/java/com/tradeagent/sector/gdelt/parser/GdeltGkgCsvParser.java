package com.tradeagent.sector.gdelt.parser;

import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class GdeltGkgCsvParser {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public List<GdeltGkgRecord> parseCsv(Path csvFile, int maxRows) {
        List<GdeltGkgRecord> records = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            int count = 0;

            while ((line = reader.readLine()) != null && count < maxRows) {
                GdeltGkgRecord record = parseLine(line);
                if (record != null) {
                    records.add(record);
                    count++;
                }
            }
        } catch (IOException ex) {
            System.out.println("[GDELT] CSV 파싱 실패: " + csvFile + " / " + ex.getMessage());
            return List.of();
        }

        return records;
    }

    private GdeltGkgRecord parseLine(String line) {
        String[] parts = line.split("\\t", -1);
        if (parts.length < 24) {
            return null;
        }

        return new GdeltGkgRecord(
                value(parts, 0),
                parseDate(value(parts, 1)),
                value(parts, 3),
                value(parts, 4),
                value(parts, 7),
                value(parts, 8),
                value(parts, 13),
                value(parts, 14),
                value(parts, 23),
                parseTone(value(parts, 15)),
                value(parts, 15)
        );
    }

    private String value(String[] parts, int index) {
        return index < parts.length ? parts[index] : "";
    }

    private LocalDateTime parseDate(String value) {
        try {
            return LocalDateTime.parse(value, DATE_FORMATTER);
        } catch (Exception ex) {
            return null;
        }
    }

    private BigDecimal parseTone(String rawTone) {
        if (rawTone == null || rawTone.isBlank()) {
            return BigDecimal.ZERO;
        }

        try {
            String first = rawTone.split(",")[0].trim();
            return new BigDecimal(first);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }
}