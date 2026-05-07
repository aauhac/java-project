package com.tradeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alpaca")
public class AlpacaProperties {

    private String baseUrl = "https://data.alpaca.markets/v2";
    private String apiKey = "";
    private String apiSecret = "";
    private String defaultSymbol = "AAPL";
    private String defaultTimeframe = "1Day";
    private int defaultLimit = 60;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getApiSecret() { return apiSecret; }
    public void setApiSecret(String apiSecret) { this.apiSecret = apiSecret; }

    public String getDefaultSymbol() { return defaultSymbol; }
    public void setDefaultSymbol(String defaultSymbol) { this.defaultSymbol = defaultSymbol; }

    public String getDefaultTimeframe() { return defaultTimeframe; }
    public void setDefaultTimeframe(String defaultTimeframe) { this.defaultTimeframe = defaultTimeframe; }

    public int getDefaultLimit() { return defaultLimit; }
    public void setDefaultLimit(int defaultLimit) { this.defaultLimit = defaultLimit; }
}
