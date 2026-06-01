package com.tradeagent.sector.gdelt;

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

    private static final String CACHE_DIR = "./data/gdelt-raw";
    private static final int TIMEOUT_SECONDS = 30;
    private final HttpClient httpClient;

    public GdeltRawFileDownloader() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path downloadIfAbsent(GdeltRawFileRef ref) {
        try {
            Path cacheDir = Paths.get(CACHE_DIR);
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve(ref.filename());
            if (Files.exists(target)) {
                return target;
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(ref.url()))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .header("User-Agent", "TradeAgent-GdeltRaw/1.0")
                    .GET()
                    .build();
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
            if (response.statusCode() != 200) {
                Files.deleteIfExists(target);
                throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                        "Failed to download GDELT file: HTTP " + response.statusCode() + " url=" + ref.url());
            }
            return target;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "Failed to download GDELT file: " + ex.getMessage());
        } catch (IOException ex) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "Failed to download GDELT file: " + ex.getMessage());
        }
    }
}
