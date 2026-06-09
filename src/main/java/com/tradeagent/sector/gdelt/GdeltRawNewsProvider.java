package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.SectorRefreshProgress;
import com.tradeagent.sector.gdelt.dto.GdeltGkgRecord;
import com.tradeagent.sector.gdelt.dto.GdeltRawSample;
import com.tradeagent.sector.gdelt.parser.GdeltGkgCsvParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GdeltRawNewsProvider {

    private static final Logger log = LoggerFactory.getLogger(GdeltRawNewsProvider.class);

    private static final int YEAR = 2026;
    private static final int DAYS = 30;
    private static final int MAX_ROWS_PER_FILE = 2000;
    private static final int MAX_CACHED_FILES = 30;
    private static final LocalTime SAMPLE_TIME = LocalTime.of(18, 0);

    private final GdeltRawFileDownloader fileDownloader;
    private final GdeltRawFileCache fileCache;
    private final GdeltGkgCsvParser csvParser;
    private final SectorRefreshProgress progress;

    public GdeltRawNewsProvider(GdeltRawFileDownloader fileDownloader,
                                GdeltRawFileCache fileCache,
                                GdeltGkgCsvParser csvParser,
                                SectorRefreshProgress progress) {
        this.fileDownloader = fileDownloader;
        this.fileCache = fileCache;
        this.csvParser = csvParser;
        this.progress = progress;
    }

    public GdeltRawSample fetchMonthlySample(LocalDate baseDate) {
        LocalDate requestedBaseDate = baseDate != null ? baseDate : LocalDate.now();
        LocalDate latestDate = requestedBaseDate.minusDays(2);
        LocalDate startDate = latestDate.minusDays(DAYS - 1);

        progress.log(
                "DOWNLOAD",
                "기준 날짜 " + requestedBaseDate + "에서 2일을 뺀 "
                        + latestDate + " 18:00 GDELT 파일부터 수집합니다."
        );

        List<Path> csvFiles = new ArrayList<>();
        List<GdeltGkgRecord> records = new ArrayList<>();

        for (int i = 0; i < DAYS; i++) {
            LocalDate date = latestDate.minusDays(i);
            String url = buildGkgUrl(date);

            progress.update(
                    "DOWNLOAD",
                    i + 1,
                    "GDELT 파일 다운로드 및 CSV 변환 중: " + (i + 1) + "/" + DAYS
            );

            try {
                Path csvFile = fileDownloader.downloadAndUnzipCsv(url);
                csvFiles.add(csvFile);

                List<GdeltGkgRecord> parsed = csvParser.parseCsv(csvFile, MAX_ROWS_PER_FILE);
                records.addAll(parsed);

                progress.log(
                        "DOWNLOAD",
                        csvFile.getFileName() + " 처리 완료 / 파싱 " + parsed.size() + "건"
                );
            } catch (Exception ex) {
                progress.log(
                        "DOWNLOAD_WARN",
                        "파일 처리 실패: " + url + " / " + ex.getMessage()
                );

                log.warn("Skipping GDELT file {}: {}", url, ex.getMessage());
            }
        }
        progress.log(
                "DOWNLOAD",
                "GDELT 다운로드/CSV 파싱 완료: 파일 " + csvFiles.size() + "개, 레코드 " + records.size() + "건"
        );
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