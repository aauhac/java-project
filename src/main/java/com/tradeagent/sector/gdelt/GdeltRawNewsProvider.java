package com.tradeagent.sector.gdelt;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
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

    private final GdeltRawProperties properties;
    private final GdeltFileListClient fileListClient;
    private final GdeltGkgFileSelector fileSelector;
    private final GdeltRawFileDownloader fileDownloader;
    private final GdeltRawFileCache fileCache;
    private final GdeltGkgCsvParser csvParser;

    public GdeltRawNewsProvider(GdeltRawProperties properties,
                                GdeltFileListClient fileListClient,
                                GdeltGkgFileSelector fileSelector,
                                GdeltRawFileDownloader fileDownloader,
                                GdeltRawFileCache fileCache,
                                GdeltGkgCsvParser csvParser) {
        this.properties = properties;
        this.fileListClient = fileListClient;
        this.fileSelector = fileSelector;
        this.fileDownloader = fileDownloader;
        this.fileCache = fileCache;
        this.csvParser = csvParser;
    }

    public GdeltRawSample fetchMonthlySample(LocalDate startDate, int days, LocalTime sampleTime) {
        if (!properties.isEnabled()) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR, "GDELT raw ingestion is disabled");
        }

        LocalDate resolvedStartDate = startDate != null ? startDate : LocalDate.now().minusDays(properties.getDefaultDays() - 1L);
        int resolvedDays = days > 0 ? Math.min(days, properties.getSelectedFilesPerRefresh()) : properties.getDefaultDays();
        LocalTime resolvedSampleTime = sampleTime != null
                ? sampleTime
                : LocalTime.of(properties.getDefaultSampleTime() / 100, properties.getDefaultSampleTime() % 100);

        List<GdeltRawFileRef> refs = fileListClient.fetchGkgFileRefs();
        List<GdeltRawFileRef> selected = fileSelector.selectDailySamples(refs, resolvedStartDate, resolvedDays, resolvedSampleTime);
        List<Path> files = new ArrayList<>();
        List<GdeltGkgRecord> records = new ArrayList<>();
        for (GdeltRawFileRef ref : selected) {
            Path file = fileDownloader.downloadIfAbsent(ref);
            files.add(file);
            records.addAll(csvParser.parse(file, properties.getMaxRowsPerFile()));
        }
        fileCache.enforceMaxFiles(properties.getMaxCachedFiles());
        return new GdeltRawSample(
                resolvedStartDate,
                resolvedDays,
                resolvedSampleTime,
                selected.size(),
                records.size(),
                files,
                records
        );
    }
}
