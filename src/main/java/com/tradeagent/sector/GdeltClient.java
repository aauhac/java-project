package com.tradeagent.sector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.GdeltRateLimitException;
import com.tradeagent.common.GdeltSupport;
import com.tradeagent.common.ValidationException;
import com.tradeagent.config.GdeltProperties;
import com.tradeagent.config.VllmProperties;
import com.tradeagent.feedback.VllmClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Path;

@Component
public class GdeltClient {

    private static final DateTimeFormatter GDELT_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Map<String, String> QUERY_BY_SECTOR = Map.of(
            "SEMI", "\"semiconductor\" OR chip OR foundry OR gpu OR NVDA OR AMD OR TSM OR NVIDIA",
            "AIINF", "\"AI infrastructure\" OR datacenter OR cloud OR compute OR NVDA OR MSFT OR AMZN OR GOOGL OR NVIDIA",
            "EV", "\"electric vehicle\" OR battery OR charging OR TSLA OR RIVN OR LI OR Tesla",
            "BIO", "biotech OR biopharma OR pharma OR drug OR trial OR FDA OR MRNA OR AMGN OR GILD OR Moderna",
            "CLOUD", "\"cloud software\" OR saas OR enterprise cloud OR microsoft azure OR oracle cloud OR salesforce OR serviceNow OR MSFT OR ORCL OR CRM OR NOW",
            "CYBER", "cybersecurity OR cyberattack OR network security OR endpoint security OR CrowdStrike OR Palo Alto Networks OR Zscaler OR CRWD OR PANW OR ZS",
            "FINPAY", "fintech OR digital payments OR payments OR card network OR Visa OR Mastercard OR PayPal OR Block OR V OR MA OR PYPL OR SQ"
    );
    private static final String BROAD_MARKET_QUERY =
            "\"semiconductor\" OR chip OR foundry OR gpu OR " +
            "\"AI infrastructure\" OR datacenter OR cloud OR compute OR \"artificial intelligence\" OR " +
            "\"electric vehicle\" OR battery OR charging OR " +
            "biotech OR biopharma OR pharma OR drug OR trial OR FDA OR " +
            "\"cloud software\" OR saas OR cybersecurity OR cyberattack OR endpoint security OR " +
            "fintech OR payments OR \"digital payments\" OR " +
            "NVDA OR AMD OR TSM OR NVIDIA OR MSFT OR AMZN OR GOOGL OR ORCL OR CRM OR NOW OR " +
            "TSLA OR RIVN OR LI OR Tesla OR MRNA OR AMGN OR GILD OR Moderna OR " +
            "CRWD OR PANW OR ZS OR V OR MA OR PYPL OR SQ";

    private final WebClient webClient;
    private final GdeltProperties gdeltProperties;
    private final VllmClient vllmClient;
    private final VllmProperties vllmProperties;
    private final ObjectMapper objectMapper;
    private final Path lastRequestFile;
    private final Path cacheDir;
    private final Duration cacheTtl;

    public GdeltClient(WebClient.Builder webClientBuilder,
                       GdeltProperties gdeltProperties,
                       VllmClient vllmClient,
                       VllmProperties vllmProperties) {
        this.gdeltProperties = gdeltProperties;
        this.vllmClient = vllmClient;
        this.vllmProperties = vllmProperties;
        this.webClient = webClientBuilder
                .baseUrl(normalizeBaseUrl(gdeltProperties.getBaseUrl()))
                .defaultHeader("User-Agent", "TradeAgent-GdeltClient/1.0")
                .build();
        this.objectMapper = new ObjectMapper();
        this.lastRequestFile = GdeltSupport.defaultLastRequestFile(gdeltProperties.getLastRequestFile());
        this.cacheDir = GdeltSupport.defaultCacheDir(gdeltProperties.getCacheDir());
        this.cacheTtl = Duration.ofHours(Math.max(gdeltProperties.getCacheTtlHours(), 1));
    }

    public List<NewsEvent> fetchSectorNews(String sectorCode, LocalDate date) {
        return fetchSectorNews(sectorCode, date, false);
    }

    public List<NewsEvent> fetchSectorNews(String sectorCode, LocalDate date, boolean forceRefresh) {
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        LocalDate resolvedDate = date != null ? date : LocalDate.now();
        return requestArticles(buildSectorQuery(resolvedSectorCode), resolvedDate, resolvedDate, 20, forceRefresh).stream()
                .map(article -> toNewsEvent(article, resolvedSectorCode, resolvedDate))
                .toList();
    }

    public List<NewsEvent> fetchMarketNews(LocalDate from, LocalDate to) {
        return fetchMarketNews(from, to, false);
    }

    public List<NewsEvent> fetchMarketNews(LocalDate from, LocalDate to, boolean forceRefresh) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(gdeltProperties.getLookbackDays());
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }

        return requestArticles(BROAD_MARKET_QUERY, resolvedFrom, resolvedTo, gdeltProperties.getMaxRecords(), forceRefresh).stream()
                .map(article -> toNewsEvent(article, "UNCLASSIFIED", resolvedTo))
                .toList();
    }

    private List<GdeltArticle> requestArticles(String query, LocalDate from, LocalDate to, int maxRecords, boolean forceRefresh) {
        String normalizedQuery = normalizeQuery(query);
        String cacheKey = GdeltSupport.cacheKey(normalizedQuery, from, to, maxRecords);
        try {
            if (!forceRefresh) {
                var cached = GdeltSupport.loadCache(cacheDir, cacheKey, cacheTtl);
                if (cached.isPresent()) {
                    try {
                        GdeltDocResponse response = parseDocResponse(cached.get().body(), 200);
                        if (response != null && response.articles() != null) {
                            return response.articles();
                        }
                    } catch (RuntimeException ex) {
                        // Ignore stale or corrupted cache and fall through to a live request.
                    }
                }
            }

            long waitMs = GdeltSupport.reserveRequestSlot(lastRequestFile, gdeltProperties.getMinRequestIntervalMs());
            RawGdeltResponse raw = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/doc/doc")
                            .queryParam("query", normalizedQuery)
                            .queryParam("mode", "ArtList")
                            .queryParam("maxrecords", maxRecords)
                            .queryParam("format", "json")
                            .queryParam("sort", "datedesc")
                            .queryParam("startdatetime", formatDateTime(from.atStartOfDay()))
                            .queryParam("enddatetime", formatDateTime(to.atTime(LocalTime.MAX)))
                            .build())
                    .exchangeToMono(response -> response.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new RawGdeltResponse(response.statusCode(), body)))
                    .block(Duration.ofSeconds(gdeltProperties.getTimeoutSeconds()));

            if (raw == null) {
                return List.of();
            }
            if (raw.statusCode().value() == 429 || isRateLimitBody(raw.body())) {
                throw new GdeltRateLimitException("GDELT 요청 제한(429)으로 동향 분석을 수행하지 못했습니다. 잠시 후 다시 시도하세요.");
            }
            if (raw.statusCode().isError()) {
                throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                        "GDELT request failed with status " + raw.statusCode().value());
            }
            GdeltDocResponse response = parseDocResponse(raw.body(), raw.statusCode().value());
            if (response == null || response.articles() == null) {
                return List.of();
            }
            if (!isRateLimitBody(raw.body()) && isLikelyJson(raw.body())) {
                GdeltSupport.saveCache(cacheDir, cacheKey, raw.body());
            }
            return response.articles();
        } catch (GdeltRateLimitException ex) {
            throw ex;
        } catch (ExternalApiException ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().value() == 429 || isRateLimitBody(ex.getResponseBodyAsString())) {
                throw new GdeltRateLimitException("GDELT 요청 제한(429)으로 동향 분석을 수행하지 못했습니다. 잠시 후 다시 시도하세요.");
            }
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "GDELT request failed with status " + ex.getStatusCode().value());
        } catch (RuntimeException ex) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR, "GDELT request failed");
        }
    }

    private GdeltDocResponse parseDocResponse(String body, int statusCode) {
        if (!StringUtils.hasText(body)) {
            return new GdeltDocResponse(List.of());
        }
        try {
            return objectMapper.readValue(body, GdeltDocResponse.class);
        } catch (IOException ex) {
            if (isRateLimitBody(body)) {
                throw new GdeltRateLimitException("GDELT 요청 제한으로 동향 분석을 수행하지 못했습니다. 잠시 후 다시 시도하세요.");
            }
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "GDELT response parse failed with status " + statusCode);
        }
    }

    private boolean isRateLimitBody(String body) {
        if (!StringUtils.hasText(body)) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.ROOT);
        return normalized.contains("please limit requests")
                || normalized.contains("one every 5 seconds")
                || normalized.contains("rate limit");
    }

    private boolean isLikelyJson(String body) {
        if (!StringUtils.hasText(body)) {
            return false;
        }
        String trimmed = body.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private NewsEvent toNewsEvent(GdeltArticle article, String sectorCode, LocalDate fallbackDate) {
        return new NewsEvent(
                sectorCode,
                inferSymbol(article.title()),
                defaultText(article.title(), sectorCode + " sector update"),
                defaultText(article.domain(), "GDELT"),
                defaultText(article.url(), "https://www.gdeltproject.org"),
                estimateTone(article.title()),
                parseSeenDate(article.seendate(), fallbackDate)
        );
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR, "GDELT baseUrl is not configured");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "query must not be blank");
        }
        String trimmed = query.trim();
        if (trimmed.contains(" OR ") && !(trimmed.startsWith("(") && trimmed.endsWith(")"))) {
            return "(" + trimmed + ")";
        }
        return trimmed;
    }

    private String normalizeSectorCode(String sectorCode) {
        if (!StringUtils.hasText(sectorCode)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "sectorCode must not be blank");
        }
        return sectorCode.trim().toUpperCase(Locale.ROOT);
    }

    private String buildSectorQuery(String sectorCode) {
        return QUERY_BY_SECTOR.getOrDefault(sectorCode, sectorCode);
    }

    private String inferSymbol(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        String upperTitle = title.toUpperCase(Locale.ROOT);
        for (String symbol : List.of("NVDA", "AMD", "TSM", "SMH", "MSFT", "AMZN", "GOOGL", "TSLA", "RIVN", "LI", "MRNA", "AMGN", "GILD", "IBB")) {
            if (upperTitle.contains(symbol)) {
                return symbol;
            }
        }
        return null;
    }

    private BigDecimal estimateTone(String title) {
        BigDecimal vllmTone = estimateToneWithVllm(title);
        if (vllmTone != null) {
            return vllmTone;
        }
        return estimateToneWithKeywords(title);
    }

    private BigDecimal estimateToneWithVllm(String title) {
        if (!vllmProperties.isEnabled() || !StringUtils.hasText(title)) {
            return null;
        }
        String response = vllmClient.generateText("""
                Return only one number between -10 and 10 for the sentiment of this finance news headline.
                Headline: %s
                """.formatted(title));
        String trimmed = response == null ? "" : response.trim();
        if (!trimmed.matches("^-?\\d+(\\.\\d+)?$")) {
            return null;
        }
        BigDecimal value = new BigDecimal(trimmed);
        if (value.compareTo(BigDecimal.TEN) > 0) {
            value = BigDecimal.TEN;
        }
        if (value.compareTo(BigDecimal.TEN.negate()) < 0) {
            value = BigDecimal.TEN.negate();
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateToneWithKeywords(String title) {
        if (!StringUtils.hasText(title)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        String normalized = title.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String positive : List.of("growth", "gain", "surge", "strong", "record", "approval", "boost", "optimism")) {
            if (normalized.contains(positive)) {
                score += 2;
            }
        }
        for (String negative : List.of("cut", "fall", "weak", "risk", "decline", "lawsuit", "slowdown", "pressure")) {
            if (normalized.contains(negative)) {
                score -= 2;
            }
        }
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime parseSeenDate(String seendate, LocalDate fallbackDate) {
        if (!StringUtils.hasText(seendate)) {
            return fallbackDate.atStartOfDay();
        }
        try {
            return LocalDateTime.parse(seendate, GDELT_DATE_TIME);
        } catch (Exception ex) {
            return fallbackDate.atStartOfDay();
        }
    }

    private String formatDateTime(LocalDateTime value) {
        return value.format(GDELT_DATE_TIME);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

record GdeltDocResponse(List<GdeltArticle> articles) {
}

record GdeltArticle(String title, String url, String domain, String seendate) {
}

record RawGdeltResponse(HttpStatusCode statusCode, String body) {
}
