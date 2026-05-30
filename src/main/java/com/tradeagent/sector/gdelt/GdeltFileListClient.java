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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GdeltFileListClient {

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(\\d{14})\\.gkg\\.csv\\.zip$");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final GdeltRawProperties properties;
    private final HttpClient httpClient;

    public GdeltFileListClient(GdeltRawProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<GdeltRawFileRef> fetchGkgFileRefs() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getMasterFileListUrl()))
                .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .header("User-Agent", "TradeAgent-GdeltRaw/1.0")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                        "Failed to fetch masterfilelist.txt: HTTP " + response.statusCode());
            }
            return response.body().lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .filter(ref -> ref != null)
                    .sorted(Comparator.comparing(GdeltRawFileRef::timestamp))
                    .toList();
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "Failed to fetch masterfilelist.txt: " + ex.getMessage());
        }
    }

    private GdeltRawFileRef parseLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            return null;
        }
        String url = parts[parts.length - 1].trim();
        if (!isAllowedGkgUrl(url)) {
            return null;
        }
        String filename = url.substring(url.lastIndexOf('/') + 1);
        Matcher matcher = TIMESTAMP_PATTERN.matcher(filename);
        if (!matcher.find()) {
            return null;
        }
        LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), TIMESTAMP_FORMATTER);
        long sizeBytes = parseLong(parts[0]);
        String hash = parts[1];
        return new GdeltRawFileRef(sizeBytes, hash, url, filename, timestamp);
    }

    private boolean isAllowedGkgUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.endsWith(".gkg.csv.zip")
                && !lower.contains(".translation.")
                && !lower.contains("export.csv")
                && !lower.contains("mentions.csv")
                && !lower.contains("gkgcounts");
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return 0L;
        }
    }
}
