package com.tradeagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeagent.common.GdeltSupport;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.net.ssl.SSLHandshakeException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GdeltDebugMain {

    private static final DateTimeFormatter GDELT_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_BASE_URL = "https://api.gdeltproject.org/api/v2";
    private static final String BROAD_MARKET_QUERY = "NVDA OR MSFT OR AMZN OR GOOGL OR TSLA";
    private static final Map<String, String> QUERY_BY_SECTOR = Map.of(
            "SEMI", "\"semiconductor\" OR chip OR foundry OR gpu OR NVDA OR AMD OR TSM OR NVIDIA",
            "AIINF", "\"AI infrastructure\" OR datacenter OR cloud OR compute OR NVDA OR MSFT OR AMZN OR GOOGL OR NVIDIA",
            "EV", "\"electric vehicle\" OR battery OR charging OR TSLA OR RIVN OR LI OR Tesla",
            "BIO", "biotech OR biopharma OR pharma OR drug OR trial OR FDA OR MRNA OR AMGN OR GILD OR Moderna",
            "CLOUD", "\"cloud software\" OR saas OR enterprise cloud OR microsoft azure OR oracle cloud OR salesforce OR serviceNow OR MSFT OR ORCL OR CRM OR NOW",
            "CYBER", "cybersecurity OR cyberattack OR network security OR endpoint security OR CrowdStrike OR Palo Alto Networks OR Zscaler OR CRWD OR PANW OR ZS",
            "FINPAY", "fintech OR digital payments OR payments OR card network OR Visa OR Mastercard OR PayPal OR Block OR V OR MA OR PYPL OR SQ"
    );

    private GdeltDebugMain() {
    }

    public static void main(String[] args) throws Exception {
        boolean force = false;
        List<String> positional = new java.util.ArrayList<>();
        for (String arg : args) {
            if ("--force".equalsIgnoreCase(arg)) {
                force = true;
            } else {
                positional.add(arg);
            }
        }
        String target = positional.isEmpty() ? "BROAD" : positional.get(0).trim().toUpperCase();
        LocalDate toDate = positional.size() > 2 ? LocalDate.parse(positional.get(2)) : LocalDate.now();
        LocalDate fromDate = positional.size() > 1 ? LocalDate.parse(positional.get(1)) : toDate.minusDays(3);
        int maxRecords = positional.size() > 3 ? Integer.parseInt(positional.get(3)) : 20;
        String baseUrl = System.getenv().getOrDefault("GDELT_BASE_URL", DEFAULT_BASE_URL);
        List<String> queries = resolveQueries(target);
        Path lastRequestFile = GdeltSupport.defaultLastRequestFile(System.getenv("GDELT_LAST_REQUEST_FILE"));
        Path cacheDir = GdeltSupport.defaultCacheDir(System.getenv("GDELT_CACHE_DIR"));
        Duration cacheTtl = Duration.ofHours(parseLong(System.getenv("GDELT_CACHE_TTL_HOURS"), 24L));
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        for (int i = 0; i < queries.size(); i++) {
            String query = queries.get(i);
            String phase = queries.size() == 1 ? target : target + " (" + (i + 1) + "/" + queries.size() + ")";
            executeQuery(client, baseUrl, query, fromDate, toDate, maxRecords, force, lastRequestFile, cacheDir, cacheTtl, phase);
            if (i < queries.size() - 1) {
                System.out.println();
            }
        }
    }

    private static List<String> resolveQueries(String target) {
        if (target.isBlank() || "BROAD".equals(target) || "ALL".equals(target)) {
            return List.of(BROAD_MARKET_QUERY);
        }
        return List.of(QUERY_BY_SECTOR.getOrDefault(target, target));
    }

    private static void executeQuery(HttpClient client,
                                     String baseUrl,
                                     String query,
                                     LocalDate fromDate,
                                     LocalDate toDate,
                                     int maxRecords,
                                     boolean force,
                                     Path lastRequestFile,
                                     Path cacheDir,
                                     Duration cacheTtl,
                                     String phase) throws Exception {
        String normalizedQuery = normalizeQuery(query);
        String cacheKey = GdeltSupport.cacheKey(normalizedQuery, fromDate, toDate, maxRecords);
        URI uri = buildUri(baseUrl, normalizedQuery, fromDate, toDate, maxRecords);
        System.out.println("=== GDELT DEBUG REQUEST ===");
        System.out.println("target      : " + phase);
        System.out.println("fromDate    : " + fromDate);
        System.out.println("toDate      : " + toDate);
        System.out.println("maxRecords  : " + maxRecords);
        System.out.println("cacheKey    : " + cacheKey);
        System.out.println("force       : " + force);

        if (!force) {
            var cached = GdeltSupport.loadCache(cacheDir, cacheKey, cacheTtl);
            if (cached.isPresent()) {
                System.out.println("cacheHit    : true");
                System.out.println("gdeltCall   : false");
                System.out.println("status      : CACHE");
                System.out.println("contentType : application/json (cached)");
                printArticles(cached.get().body());
                return;
            }
        }

        long waitMs = GdeltSupport.reserveRequestSlot(lastRequestFile,
                parseLong(System.getenv("GDELT_MIN_REQUEST_INTERVAL_MS"), 60000L));
        System.out.println("cacheHit    : false");
        System.out.println("gdeltCall   : true");
        System.out.println("waitMs      : " + waitMs);
        System.out.println("requestUri  : " + uri);
        System.out.println();

        HttpRequest request = HttpRequest.newBuilder(uri)
                .version(HttpClient.Version.HTTP_1_1)
                .header("Accept", "application/json")
                .header("User-Agent", "TradeAgent-GdeltDebugMain/1.0")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (SSLHandshakeException ex) {
            System.out.println("status      : SSL_HANDSHAKE_FAILED");
            System.out.println("contentType : -");
            System.out.println("=== ERROR BODY ===");
            System.out.println("GDELT와 TLS 핸드셰이크가 실패했습니다.");
            System.out.println("원인: 서버 차단, 프록시, 네트워크 보안장비, 또는 Java HttpClient 지문 문제일 수 있습니다.");
            return;
        }
        System.out.println("status      : " + response.statusCode());
        System.out.println("contentType : " + response.headers().firstValue("content-type").orElse("-"));

        if (response.statusCode() != 200) {
            System.out.println("=== ERROR BODY ===");
            System.out.println(response.body());
            if (response.statusCode() == 429) {
                System.out.println();
                System.out.println("GDELT rate limit(429)입니다. 앱에서 기사 0건으로 보이는 원인일 가능성이 큽니다.");
            }
            return;
        }

        if (isRateLimitLikeBody(response.body())) {
            System.out.println();
            System.out.println("GDELT 제한/차단 응답으로 보입니다(HTTP 200 + 안내문).");
            System.out.println("요청 간격을 늘리고(60초+), 동일 요청은 캐시를 사용하세요.");
            System.out.println("=== RAW BODY (head) ===");
            System.out.println(preview(response.body(), 800));
            return;
        }

        if (isLikelyJson(response.body())) {
            GdeltSupport.saveCache(cacheDir, cacheKey, response.body());
        }
        printArticles(response.body());
    }

    private static URI buildUri(String baseUrl, String query, LocalDate fromDate, LocalDate toDate, int maxRecords) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", normalizeQuery(query));
        params.put("mode", "ArtList");
        params.put("maxrecords", String.valueOf(maxRecords));
        params.put("format", "json");
        params.put("sort", "datedesc");
        params.put("startdatetime", formatDateTime(fromDate.atStartOfDay()));
        params.put("enddatetime", formatDateTime(toDate.atTime(LocalTime.MAX)));

        StringBuilder builder = new StringBuilder(trimTrailingSlash(baseUrl))
                .append("/doc/doc?");
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                builder.append('&');
            }
            first = false;
            builder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return URI.create(builder.toString());
    }

    private static void printArticles(String body) {
        if (!isLikelyJson(body)) {
            System.out.println("응답이 JSON이 아닙니다. GDELT 안내/차단 응답일 수 있습니다.");
            System.out.println("=== RAW BODY (head) ===");
            System.out.println(preview(body, 800));
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception ex) {
            System.out.println("JSON 파싱 실패: " + ex.getMessage());
            System.out.println("=== RAW BODY (head) ===");
            System.out.println(preview(body, 800));
            return;
        }
        JsonNode articles = root.path("articles");

        if (!articles.isArray() || articles.isEmpty()) {
            System.out.println("articles    : 0");
            System.out.println("응답은 성공했지만 기사 배열이 비어 있습니다.");
            System.out.println("=== RAW BODY ===");
            System.out.println(body);
            return;
        }

        System.out.println("articles    : " + articles.size());
        System.out.println("=== ARTICLE LIST ===");
        int index = 1;
        for (JsonNode article : articles) {
            System.out.println("[" + index++ + "] " + text(article, "title"));
            System.out.println("    url      : " + text(article, "url"));
            System.out.println("    domain   : " + text(article, "domain"));
            System.out.println("    seendate : " + text(article, "seendate"));
            System.out.println("    language : " + text(article, "language"));
            System.out.println("    country  : " + text(article, "sourcecountry"));
            System.out.println();
        }
    }

    private static boolean isLikelyJson(String body) {
        if (body == null) {
            return false;
        }
        String trimmed = body.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }

    private static boolean isRateLimitLikeBody(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String normalized = body.toLowerCase();
        return normalized.contains("please limit requests")
                || normalized.contains("one every 5 seconds")
                || normalized.contains("rate limit")
                || normalized.startsWith("your ");
    }

    private static String preview(String text, int maxLength) {
        if (text == null) {
            return "-";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...(truncated)";
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.contains(" OR ") && !(trimmed.startsWith("(") && trimmed.endsWith(")"))) {
            return "(" + trimmed + ")";
        }
        return trimmed;
    }

    private static long parseLong(String value, long defaultValue) {
        try {
            return value == null ? defaultValue : Long.parseLong(value.trim());
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode field = node.path(fieldName);
        return field.isMissingNode() || field.isNull() ? "-" : field.asText("-");
    }

    private static String trimTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value.format(GDELT_DATE_TIME);
    }
}
