package com.tradeagent.chart;

import com.tradeagent.chart.ChartModels.ChartBar;
import com.tradeagent.chart.ChartModels.ChartResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ChartService {

    private static final Logger log = LoggerFactory.getLogger(ChartService.class);
    private static final DateTimeFormatter ALPACA_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ChartProperties properties;
    private final RestClient restClient;

    public ChartService(ChartProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().build();
    }

    public ChartResponse fetchBars(String symbol, String timeframe, LocalDate start, LocalDate end) {
        String resolvedSymbol = normalizeSymbol(symbol);
        String resolvedTimeframe = normalizeTimeframe(timeframe);
        String apiKey = normalizeCredential(properties.getApiKey());
        String apiSecret = normalizeCredential(properties.getApiSecret());
        LocalDate resolvedEnd = end != null ? end : LocalDate.now(ZoneOffset.UTC);
        int defaultSpan = Math.max(properties.getDefaultLimit() - 1, 30);
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(defaultSpan);

        if (resolvedStart.isAfter(resolvedEnd)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start must be on or before end");
        }

        URI uri = UriComponentsBuilder.fromUriString(normalizeBaseUrl())
                .pathSegment("stocks", resolvedSymbol, "bars")
                .queryParam("timeframe", resolvedTimeframe)
                .queryParam("start", toIsoStart(resolvedStart))
                .queryParam("end", toIsoStart(resolvedEnd.plusDays(1)))
                .queryParam("limit", calculateLimit(resolvedStart, resolvedEnd))
                .queryParam("adjustment", "raw")
                .queryParam("feed", "iex")
                .build(true)
                .toUri();

        log.info("Fetching bars from Alpaca: symbol={}, timeframe={}, start={}, end={}, uri={}, auth={}",
                resolvedSymbol, resolvedTimeframe, resolvedStart, resolvedEnd, uri,
                describeCredentials(apiKey, apiSecret));

        AlpacaBarsResponse response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .header("APCA-API-KEY-ID", apiKey)
                    .header("APCA-API-SECRET-KEY", apiSecret)
                    .retrieve()
                    .body(AlpacaBarsResponse.class);
            log.info("Alpaca API call successful, bars count: {}", response == null ? 0 : (response.bars() == null ? 0 : response.bars().size()));
        } catch (RestClientResponseException ex) {
            log.error("Alpaca API HTTP error - Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString(), ex);
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Alpaca API authentication failed (401). Check API key/secret, remove accidental whitespace, and verify the correct data endpoint for your credentials. Current baseUrl="
                                + normalizeBaseUrl() + ", auth=" + describeCredentials(apiKey, apiSecret)
                                + ". If these are paper/sandbox credentials, try alpaca.base-url=https://data.sandbox.alpaca.markets/v2",
                        ex);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Alpaca API error (HTTP " + ex.getStatusCode() + "): " + ex.getResponseBodyAsString(), ex);
        } catch (RestClientException ex) {
            log.error("Alpaca API connection error", ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to reach Alpaca API: " + ex.getMessage(), ex);
        }

        List<ChartBar> bars = response == null || response.bars() == null
                ? List.of()
                : response.bars().stream()
                .map(payload -> new ChartBar(
                        payload.t(),
                        payload.o().doubleValue(),
                        payload.h().doubleValue(),
                        payload.l().doubleValue(),
                        payload.c().doubleValue(),
                        payload.v() == null ? 0L : payload.v()))
                .toList();

        return new ChartResponse(resolvedSymbol, resolvedTimeframe, resolvedStart, resolvedEnd, bars);
    }

    private String normalizeSymbol(String symbol) {
        String candidate = StringUtils.hasText(symbol) ? symbol : properties.getDefaultSymbol();
        return candidate == null ? "AAPL" : candidate.trim().toUpperCase();
    }

    private String normalizeTimeframe(String timeframe) {
        String candidate = StringUtils.hasText(timeframe) ? timeframe : properties.getDefaultTimeframe();
        return candidate == null ? "1Day" : candidate.trim();
    }

    private String normalizeBaseUrl() {
        String baseUrl = properties.getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            return "https://data.alpaca.markets/v2";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String normalizeCredential(String value) {
        return value == null ? "" : value.trim();
    }

    private String describeCredentials(String apiKey, String apiSecret) {
        return "key=" + summarizeCredential(apiKey) + ", secret=" + summarizeCredential(apiSecret);
    }

    private String summarizeCredential(String value) {
        if (!StringUtils.hasText(value)) {
            return "<empty>";
        }
        String trimmed = value.trim();
        String prefix = trimmed.substring(0, Math.min(3, trimmed.length()));
        return prefix + "...(len=" + trimmed.length() + ")";
    }

    private String toIsoStart(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC).format(ALPACA_DATE_TIME);
    }

    private long calculateLimit(LocalDate start, LocalDate end) {
        long days = end.toEpochDay() - start.toEpochDay() + 1;
        return Math.max(days, 1);
    }
}
