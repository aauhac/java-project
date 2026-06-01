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

        // 기본 범위: 어제(D-1) ~ 30일 전(D-30), 존재하는 파일만 선택
        int resolvedDays = (days > 0 && days <= properties.getSelectedFilesPerRefresh())
                ? days
                : Math.min(properties.getDefaultDays(), properties.getSelectedFilesPerRefresh());
        // startDate가 없으면 오늘-resolvedDays일 (= 어제가 마지막 날이 되도록 today-30 기준)
        LocalDate resolvedStartDate = startDate != null
                ? startDate
                : LocalDate.now().minusDays(resolvedDays); // today-30 → range: [today-30, today-1]
        LocalTime resolvedSampleTime = sampleTime != null
                ? sampleTime
                : LocalTime.of(properties.getDefaultSampleTime() / 100, properties.getDefaultSampleTime() % 100);

        List<GdeltRawFileRef> refs = fileListClient.fetchGkgFileRefs();
        // masterfilelist에서 존재하는 파일 기준으로 하루 1개씩 선택
        List<GdeltRawFileRef> selected = fileSelector.selectDailySamples(refs, resolvedStartDate, resolvedDays, resolvedSampleTime);
        List<Path> files = new ArrayList<>();
        List<GdeltGkgRecord> records = new ArrayList<>();
        for (GdeltRawFileRef ref : selected) {
            try {
                Path file = fileDownloader.downloadIfAbsent(ref);
                files.add(file);
                records.addAll(csvParser.parse(file, properties.getMaxRowsPerFile()));
            } catch (Exception ex) {
                // 개별 파일 실패는 건너뛰고 계속
                org.slf4j.LoggerFactory.getLogger(GdeltRawNewsProvider.class)
                        .warn("Skipping GDELT file {}: {}", ref.filename(), ex.getMessage());
            }
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
