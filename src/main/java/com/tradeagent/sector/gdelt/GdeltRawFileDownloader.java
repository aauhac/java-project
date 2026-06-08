package com.tradeagent.sector.gdelt;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.sector.gdelt.dto.GdeltRawFileRef;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Component
public class GdeltRawFileDownloader {

    private static final String ZIP_DIR = "./data/gdelt-raw/zip";
    private static final String CSV_DIR = "./data/gdelt-raw/csv";
    private static final int TIMEOUT_SECONDS = 30;

    private final HttpClient httpClient;

    public GdeltRawFileDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path downloadAndUnzipCsv(String url) {
        try {
            Files.createDirectories(Paths.get(ZIP_DIR));
            Files.createDirectories(Paths.get(CSV_DIR));

            String zipFileName = url.substring(url.lastIndexOf('/') + 1);
            String csvFileName = zipFileName.replace(".zip", "");

            Path zipPath = Paths.get(ZIP_DIR).resolve(zipFileName);
            Path csvPath = Paths.get(CSV_DIR).resolve(csvFileName);

            if (Files.exists(csvPath)) {
                return csvPath;
            }

            if (!Files.exists(zipPath)) {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                        .header("User-Agent", "TradeAgent-GdeltRaw/1.0")
                        .GET()
                        .build();

                HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(zipPath));

                if (response.statusCode() != 200) {
                    Files.deleteIfExists(zipPath);
                    throw new RuntimeException("HTTP " + response.statusCode());
                }
            }

            unzipFirstEntry(zipPath, csvPath);
            return csvPath;

        } catch (Exception ex) {
            throw new RuntimeException("GDELT 파일 다운로드/압축해제 실패: " + url, ex);
        }
    }

    private void unzipFirstEntry(Path zipPath, Path csvPath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry = zis.getNextEntry();
            if (entry == null) {
                throw new IOException("zip 내부에 csv 파일이 없습니다: " + zipPath);
            }
            Files.copy(zis, csvPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}