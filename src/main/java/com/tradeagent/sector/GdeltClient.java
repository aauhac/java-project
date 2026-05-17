package com.tradeagent.sector;

import com.tradeagent.common.ErrorCode;
import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.config.GdeltProperties;
import com.tradeagent.config.VllmProperties;
import com.tradeagent.feedback.VllmClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

    public GdeltClient(WebClient.Builder webClientBuilder,
                       GdeltProperties gdeltProperties,
                       VllmClient vllmClient,
                       VllmProperties vllmProperties) {
        this.gdeltProperties = gdeltProperties;
        this.vllmClient = vllmClient;
        this.vllmProperties = vllmProperties;
        this.webClient = webClientBuilder.baseUrl(normalizeBaseUrl(gdeltProperties.getBaseUrl())).build();
    }

    public List<NewsEvent> fetchSectorNews(String sectorCode, LocalDate date) {
        String resolvedSectorCode = normalizeSectorCode(sectorCode);
        LocalDate resolvedDate = date != null ? date : LocalDate.now();
        return requestArticles(buildSectorQuery(resolvedSectorCode), resolvedDate, resolvedDate, 20).stream()
                .map(article -> toNewsEvent(article, resolvedSectorCode, resolvedDate))
                .toList();
    }

    public List<NewsEvent> fetchMarketNews(LocalDate from, LocalDate to) {
        LocalDate resolvedFrom = from != null ? from : LocalDate.now().minusDays(gdeltProperties.getLookbackDays());
        LocalDate resolvedTo = to != null ? to : LocalDate.now();
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException(ErrorCode.INVALID_INPUT, "from must be on or before to");
        }

        return requestArticles(BROAD_MARKET_QUERY, resolvedFrom, resolvedTo, gdeltProperties.getMaxRecords()).stream()
                .map(article -> toNewsEvent(article, "UNCLASSIFIED", resolvedTo))
                .toList();
    }

    private List<GdeltArticle> requestArticles(String query, LocalDate from, LocalDate to, int maxRecords) {
        try {
            GdeltDocResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/doc/doc")
                            .queryParam("query", query)
                            .queryParam("mode", "ArtList")
                            .queryParam("maxrecords", maxRecords)
                            .queryParam("format", "json")
                            .queryParam("sort", "datedesc")
                            .queryParam("startdatetime", formatDateTime(from.atStartOfDay()))
                            .queryParam("enddatetime", formatDateTime(to.atTime(LocalTime.MAX)))
                            .build())
                    .retrieve()
                    .bodyToMono(GdeltDocResponse.class)
                    .block(Duration.ofSeconds(gdeltProperties.getTimeoutSeconds()));

            if (response == null || response.articles() == null) {
                return List.of();
            }
            return response.articles();
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "GDELT request failed with status " + ex.getStatusCode().value());
        } catch (RuntimeException ex) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR, "GDELT request failed");
        }
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
