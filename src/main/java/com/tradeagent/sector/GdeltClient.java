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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GdeltClient {

    private static final DateTimeFormatter GDELT_SEEN_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Map<String, String> QUERY_BY_SECTOR = Map.of(
            "SEMI", "\"semiconductor\" OR chip OR foundry OR NVDA OR AMD OR TSM",
            "AIINF", "\"AI infrastructure\" OR datacenter OR cloud AI OR NVDA OR MSFT OR AMZN",
            "EV", "\"electric vehicle\" OR EV OR battery OR TSLA OR RIVN OR LI",
            "BIO", "biotech OR pharma OR drug OR MRNA OR AMGN OR GILD"
    );

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
        try {
            GdeltDocResponse response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/doc/doc")
                            .queryParam("query", buildQuery(resolvedSectorCode))
                            .queryParam("mode", "ArtList")
                            .queryParam("maxrecords", 10)
                            .queryParam("format", "json")
                            .queryParam("sort", "datedesc")
                            .build())
                    .retrieve()
                    .bodyToMono(GdeltDocResponse.class)
                    .block(Duration.ofSeconds(gdeltProperties.getTimeoutSeconds()));

            if (response == null || response.articles() == null || response.articles().isEmpty()) {
                return List.of();
            }

            return response.articles().stream()
                    .map(article -> new NewsEvent(
                            resolvedSectorCode,
                            inferSymbol(article.title(), resolvedSectorCode),
                            defaultText(article.title(), resolvedSectorCode + " sector update"),
                            defaultText(article.domain(), "GDELT"),
                            defaultText(article.url(), "https://www.gdeltproject.org"),
                            estimateTone(article.title()),
                            parseSeenDate(article.seendate(), resolvedDate)
                    ))
                    .toList();
        } catch (WebClientResponseException ex) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR,
                    "GDELT request failed with status " + ex.getStatusCode().value());
        } catch (RuntimeException ex) {
            throw new ExternalApiException(ErrorCode.GDELT_API_ERROR, "GDELT request failed");
        }
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

    private String buildQuery(String sectorCode) {
        return QUERY_BY_SECTOR.getOrDefault(sectorCode, sectorCode);
    }

    private String inferSymbol(String title, String sectorCode) {
        if (!StringUtils.hasText(title)) {
            return switch (sectorCode) {
                case "SEMI" -> "NVDA";
                case "AIINF" -> "MSFT";
                case "EV" -> "TSLA";
                case "BIO" -> "MRNA";
                default -> null;
            };
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
            return LocalDateTime.parse(seendate, GDELT_SEEN_DATE);
        } catch (Exception ex) {
            return fallbackDate.atStartOfDay();
        }
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}

record GdeltDocResponse(List<GdeltArticle> articles) {
}

record GdeltArticle(String title, String url, String domain, String seendate) {
}
