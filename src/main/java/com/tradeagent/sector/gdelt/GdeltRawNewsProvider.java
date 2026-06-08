package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.GdeltRawSample;
import com.tradeagent.sector.gdelt.parser.GdeltGkgCsvParser;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class GdeltRawNewsProvider {

    private static final int YEAR = 2026;
    private static final int DAYS = 30;
    private static final int MAX_ROWS_PER_FILE = 2000;
    private static final int MAX_CACHED_FILES = 30;
    private static final LocalTime SAMPLE_TIME = LocalTime.of(18, 0);

    private final GdeltRawFileDownloader fileDownloader;
    private final GdeltRawFileCache fileCache;
    private final GdeltGkgCsvParser csvParser;

    public GdeltRawNewsProvider(GdeltRawFileDownloader fileDownloader,
                                GdeltRawFileCache fileCache,
                                GdeltGkgCsvParser csvParser) {
        this.fileDownloader = fileDownloader;
        this.fileCache = fileCache;
        this.csvParser = csvParser;
    }

    public GdeltRawSample fetchMonthlySample() {
        LocalDate latestDate = LocalDate.now().minusDays(1);
        LocalDate startDate = latestDate.minusDays(DAYS - 1);

        List<Path> csvFiles = new ArrayList<>();
        List<GdeltGkgRecord> records = new ArrayList<>();

        for (int i = 0; i < DAYS; i++) {
            LocalDate date = latestDate.minusDays(i);
            String url = buildGkgUrl(date);

            try {
                Path csvFile = fileDownloader.downloadAndUnzipCsv(url);
                csvFiles.add(csvFile);
                records.addAll(csvParser.parseCsv(csvFile, MAX_ROWS_PER_FILE));
            } catch (Exception ex) {
                org.slf4j.LoggerFactory.getLogger(GdeltRawNewsProvider.class)
                        .warn("Skipping GDELT file {}: {}", url, ex.getMessage());
            }
        }

        fileCache.enforceMaxFiles(MAX_CACHED_FILES);

        return new GdeltRawSample(
                startDate,
                DAYS,
                SAMPLE_TIME,
                csvFiles.size(),
                records.size(),
                csvFiles,
                records
        );
    }

    private String buildGkgUrl(LocalDate date) {
        String mmdd = date.format(java.time.format.DateTimeFormatter.ofPattern("MMdd"));
        return "http://data.gdeltproject.org/gdeltv2/" + YEAR + mmdd + "180000.gkg.csv.zip";
    }
}