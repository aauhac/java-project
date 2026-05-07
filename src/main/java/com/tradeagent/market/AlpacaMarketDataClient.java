package com.tradeagent.market;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.config.AlpacaProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class AlpacaMarketDataClient extends AbstractMarketDataClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final DateTimeFormatter ISO_OFFSET_DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final WebClient webClient;
    private final AlpacaProperties properties;
    private final MarketDataMapper marketDataMapper;

    public AlpacaMarketDataClient(WebClient.Builder webClientBuilder,
                                  AlpacaProperties properties,
                                  MarketDataMapper marketDataMapper) {
        this.properties = properties;
        this.marketDataMapper = marketDataMapper;
        this.webClient = webClientBuilder.baseUrl(normalizeBaseUrl(properties.getBaseUrl())).build();
    }

    @Override
    public LatestQuote fetchLatestQuote(String symbol) {
        String resolvedSymbol = normalizeSymbol(symbol);
        ensureCredentials();

        try {
            AlpacaSnapshotResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/stocks/{symbol}/snapshot").build(resolvedSymbol))
                    .headers(this::applyAuthHeaders)
                    .retrieve()
                    .bodyToMono(AlpacaSnapshotResponse.class)
                    .block(REQUEST_TIMEOUT);

            if (response == null) {
                throw new ExternalApiException(ErrorCode.ALPACA_API_ERROR, "Alpaca snapshot response was empty");
            }

            return marketDataMapper.toLatestQuote(resolvedSymbol, response);
        } catch (Exception e) {
            throw handleApiError(e);
        }
    }

    @Override
    public List<PriceBar> fetchHistoricalBars(String symbol, LocalDate from, LocalDate to) {
        String resolvedSymbol = normalizeSymbol(symbol);
        validateDateRange(from, to);
        ensureCredentials();

        try {
            AlpacaBarsResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/stocks/{symbol}/bars")
                            .queryParam("timeframe", resolveTimeframe())
                            .queryParam("start", toIsoStart(from))
                            .queryParam("end", toIsoStart(to.plusDays(1)))
                            .queryParam("limit", calculateLimit(from, to))
                            .queryParam("adjustment", "raw")
                            .queryParam("feed", "iex")
                            .queryParam("sort", "asc")
                            .build(resolvedSymbol))
                    .headers(this::applyAuthHeaders)
                    .retrieve()
                    .bodyToMono(AlpacaBarsResponse.class)
                    .block(REQUEST_TIMEOUT);

            return marketDataMapper.toPriceBars(resolvedSymbol, response);
        } catch (Exception e) {
            throw handleApiError(e);
        }
    }

    private void ensureCredentials() {
        if (!StringUtils.hasText(properties.getApiKey()) || !StringUtils.hasText(properties.getApiSecret())) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "Alpaca API credentials are not configured");
        }
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from and to dates are required");
        }
        if (from.isAfter(to)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }
    }

    private String resolveTimeframe() {
        if (StringUtils.hasText(properties.getDefaultTimeframe())) {
            return properties.getDefaultTimeframe().trim();
        }
        return "1Day";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "https://data.alpaca.markets/v2";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String toIsoStart(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC).format(ISO_OFFSET_DATE_TIME);
    }

    private long calculateLimit(LocalDate from, LocalDate to) {
        return Math.max(1, ChronoUnit.DAYS.between(from, to) + 1);
    }

    private void applyAuthHeaders(HttpHeaders headers) {
        headers.set("APCA-API-KEY-ID", properties.getApiKey());
        headers.set("APCA-API-SECRET-KEY", properties.getApiSecret());
    }
}
