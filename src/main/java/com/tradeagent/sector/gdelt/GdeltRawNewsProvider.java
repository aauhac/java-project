package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.GdeltRawFileRef;
import com.tradeagent.sector.gdelt.dto.GdeltRawSample;
import com.tradeagent.sector.gdelt.parser.GdeltGkgCsvParser;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GdeltRawNewsProvider {

    private static final int DAYS = 31;
    private static final LocalTime SAMPLE_TIME = LocalTime.of(18, 0);
    private static final int MAX_ROWS_PER_FILE = 2000;
    private static final int MAX_CACHED_FILES = 30;

    private final GdeltFileListClient fileListClient;
    private final GdeltGkgFileSelector fileSelector;
    private final GdeltRawFileDownloader fileDownloader;
    private final GdeltRawFileCache fileCache;
    private final GdeltGkgCsvParser csvParser;

    public GdeltRawNewsProvider(GdeltFileListClient fileListClient,
                                GdeltGkgFileSelector fileSelector,
                                GdeltRawFileDownloader fileDownloader,
                                GdeltRawFileCache fileCache,
                                GdeltGkgCsvParser csvParser) {
        this.fileListClient = fileListClient;
        this.fileSelector = fileSelector;
        this.fileDownloader = fileDownloader;
        this.fileCache = fileCache;
        this.csvParser = csvParser;
    }

    public GdeltRawSample fetchMonthlySample() {
        LocalDate startDate = LocalDate.now().minusDays(DAYS);

        List<GdeltRawFileRef> refs = fileListClient.fetchGkgFileRefs();
        List<GdeltRawFileRef> selected = fileSelector.selectDailySamples(refs, startDate, DAYS, SAMPLE_TIME);
        List<Path> files = new ArrayList<>();
        List<GdeltGkgRecord> records = new ArrayList<>();
        for (GdeltRawFileRef ref : selected) {
            try {
                Path file = fileDownloader.downloadIfAbsent(ref);
                files.add(file);
                records.addAll(csvParser.parse(file, MAX_ROWS_PER_FILE));
            } catch (Exception ex) {
                // 개별 파일 실패는 건너뛰고 계속
                org.slf4j.LoggerFactory.getLogger(GdeltRawNewsProvider.class)
                        .warn("Skipping GDELT file {}: {}", ref.filename(), ex.getMessage());
            }
        }
        fileCache.enforceMaxFiles(MAX_CACHED_FILES);
        return new GdeltRawSample(
                startDate,
                30,
                SAMPLE_TIME,
                selected.size(),
                records.size(),
                files,
                records
        );
    }
}