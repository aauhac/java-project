package com.tradeagent.sector;

import com.tradeagent.common.ExternalApiException;
import com.tradeagent.common.ValidationException;
import com.tradeagent.config.GdeltProperties;
import com.tradeagent.common.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

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

    public GdeltClient(WebClient.Builder webClientBuilder, GdeltProperties gdeltProperties) {
        this.gdeltProperties = gdeltProperties;
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
                return buildFallbackNews(resolvedSectorCode, resolvedDate);
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
        } catch (Exception ex) {
            return buildFallbackNews(resolvedSectorCode, resolvedDate);
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

    private List<NewsEvent> buildFallbackNews(String sectorCode, LocalDate date) {
        return switch (sectorCode) {
            case "SEMI" -> List.of(
                    fallback(sectorCode, "NVDA", "Semiconductor demand remains firm across AI servers.", date, BigDecimal.valueOf(4.2)),
                    fallback(sectorCode, "TSM", "Foundry utilization improves as chip orders rebound.", date.minusDays(1), BigDecimal.valueOf(3.4))
            );
            case "AIINF" -> List.of(
                    fallback(sectorCode, "MSFT", "Cloud infrastructure spending supports AI platform growth.", date, BigDecimal.valueOf(3.8)),
                    fallback(sectorCode, "AMZN", "Datacenter expansion plans lift AI infrastructure sentiment.", date.minusDays(1), BigDecimal.valueOf(2.9))
            );
            case "EV" -> List.of(
                    fallback(sectorCode, "TSLA", "Electric vehicle demand stays mixed amid pricing pressure.", date, BigDecimal.valueOf(-1.8)),
                    fallback(sectorCode, "RIVN", "Battery supply costs remain volatile for EV makers.", date.minusDays(1), BigDecimal.valueOf(-1.2))
            );
            case "BIO" -> List.of(
                    fallback(sectorCode, "MRNA", "Biotech pipeline optimism improves on trial updates.", date, BigDecimal.valueOf(2.5)),
                    fallback(sectorCode, "AMGN", "Drug approval expectations support biotech sentiment.", date.minusDays(1), BigDecimal.valueOf(2.1))
            );
            default -> List.of(fallback(sectorCode, null, sectorCode + " sector update", date, BigDecimal.ZERO));
        };
    }

    private NewsEvent fallback(String sectorCode, String symbol, String title, LocalDate date, BigDecimal toneScore) {
        return new NewsEvent(
                sectorCode,
                symbol,
                title,
                "fallback",
                "https://www.gdeltproject.org",
                toneScore.setScale(2, RoundingMode.HALF_UP),
                date.atStartOfDay()
        );
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
