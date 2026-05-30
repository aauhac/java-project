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

    private final GdeltRawProperties properties;
    private final HttpClient httpClient;

    public GdeltRawFileDownloader(GdeltRawProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Path downloadIfAbsent(GdeltRawFileRef ref) {
        try {
            Path cacheDir = Paths.get(properties.getCacheDir());
            Files.createDirectories(cacheDir);
            Path target = cacheDir.resolve(ref.filename());
            if (Files.exists(target)) {
                return target;
            }

            HttpRequest request = HttpRequest.newBuilder(URI.create(ref.url()))
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
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
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "Failed to download GDELT file: " + ex.getMessage());
        }
    }
}
