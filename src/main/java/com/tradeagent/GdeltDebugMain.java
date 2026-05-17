package com.tradeagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GdeltDebugMain {

    private static final DateTimeFormatter GDELT_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_BASE_URL = "https://api.gdeltproject.org/api/v2";
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
        String target = args.length > 0 ? args[0].trim().toUpperCase() : "BROAD";
        LocalDate toDate = args.length > 2 ? LocalDate.parse(args[2]) : LocalDate.now();
        LocalDate fromDate = args.length > 1 ? LocalDate.parse(args[1]) : toDate.minusDays(3);
        int maxRecords = args.length > 3 ? Integer.parseInt(args[3]) : 20;
        String baseUrl = System.getenv().getOrDefault("GDELT_BASE_URL", DEFAULT_BASE_URL);
        String query = resolveQuery(target);

        URI uri = buildUri(baseUrl, query, fromDate, toDate, maxRecords);
        System.out.println("=== GDELT DEBUG REQUEST ===");
        System.out.println("target      : " + target);
        System.out.println("fromDate    : " + fromDate);
        System.out.println("toDate      : " + toDate);
        System.out.println("maxRecords  : " + maxRecords);
        System.out.println("requestUri  : " + uri);
        System.out.println();

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("status      : " + response.statusCode());

        if (response.statusCode() != 200) {
            System.out.println("=== ERROR BODY ===");
            System.out.println(response.body());
            if (response.statusCode() == 429) {
                System.out.println();
                System.out.println("GDELT rate limit(429)입니다. 앱에서 기사 0건으로 보이는 원인일 가능성이 큽니다.");
            }
            return;
        }

        printArticles(response.body());
    }

    private static String resolveQuery(String target) {
        if (target.isBlank() || "BROAD".equals(target) || "ALL".equals(target)) {
            return BROAD_MARKET_QUERY;
        }
        return QUERY_BY_SECTOR.getOrDefault(target, target);
    }

    private static URI buildUri(String baseUrl, String query, LocalDate fromDate, LocalDate toDate, int maxRecords) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", query);
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

    private static void printArticles(String body) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(body);
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
