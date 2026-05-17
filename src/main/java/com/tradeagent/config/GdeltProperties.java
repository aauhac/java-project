package com.tradeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gdelt")
public class GdeltProperties {

    private String baseUrl = "https://api.gdeltproject.org/api/v2";
    private int timeoutSeconds = 10;
    private int lookbackDays = 3;
    private int maxRecords = 80;
    private String lastRequestFile = "data/gdelt/gdelt-last-request.txt";
    private String cacheDir = "data/gdelt-cache";
    private long minRequestIntervalMs = 60000L;
    private int cacheTtlHours = 24;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public int getLookbackDays() { return lookbackDays; }
    public void setLookbackDays(int lookbackDays) { this.lookbackDays = lookbackDays; }

    public int getMaxRecords() { return maxRecords; }
    public void setMaxRecords(int maxRecords) { this.maxRecords = maxRecords; }

    public String getLastRequestFile() { return lastRequestFile; }
    public void setLastRequestFile(String lastRequestFile) { this.lastRequestFile = lastRequestFile; }

    public String getCacheDir() { return cacheDir; }
    public void setCacheDir(String cacheDir) { this.cacheDir = cacheDir; }

    public long getMinRequestIntervalMs() { return minRequestIntervalMs; }
    public void setMinRequestIntervalMs(long minRequestIntervalMs) { this.minRequestIntervalMs = minRequestIntervalMs; }

    public int getCacheTtlHours() { return cacheTtlHours; }
    public void setCacheTtlHours(int cacheTtlHours) { this.cacheTtlHours = cacheTtlHours; }
}
