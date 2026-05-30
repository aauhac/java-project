package com.tradeagent.sector.gdelt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gdelt.raw")
public class GdeltRawProperties {

    private boolean enabled = true;
    private String masterFileListUrl = "https://data.gdeltproject.org/gdeltv2/masterfilelist.txt";
    private String cacheDir = "./data/gdelt-raw";
    private int defaultDays = 30;
    private int defaultSampleTime = 1930;
    private int maxCachedFiles = 30;
    private int maxRowsPerFile = 2000;
    private int selectedFilesPerRefresh = 30;
    private int requestTimeoutSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMasterFileListUrl() {
        return masterFileListUrl;
    }

    public void setMasterFileListUrl(String masterFileListUrl) {
        this.masterFileListUrl = masterFileListUrl;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public int getDefaultDays() {
        return defaultDays;
    }

    public void setDefaultDays(int defaultDays) {
        this.defaultDays = defaultDays;
    }

    public int getDefaultSampleTime() {
        return defaultSampleTime;
    }

    public void setDefaultSampleTime(int defaultSampleTime) {
        this.defaultSampleTime = defaultSampleTime;
    }

    public int getMaxCachedFiles() {
        return maxCachedFiles;
    }

    public void setMaxCachedFiles(int maxCachedFiles) {
        this.maxCachedFiles = maxCachedFiles;
    }

    public int getMaxRowsPerFile() {
        return maxRowsPerFile;
    }

    public void setMaxRowsPerFile(int maxRowsPerFile) {
        this.maxRowsPerFile = maxRowsPerFile;
    }

    public int getSelectedFilesPerRefresh() {
        return selectedFilesPerRefresh;
    }

    public void setSelectedFilesPerRefresh(int selectedFilesPerRefresh) {
        this.selectedFilesPerRefresh = selectedFilesPerRefresh;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
}
