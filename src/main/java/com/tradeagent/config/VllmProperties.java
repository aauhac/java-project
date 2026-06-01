package com.tradeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vllm")
public class VllmProperties {

    private boolean enabled;
    private String baseUrl = "http://localhost:8000";
    private String model = "Qwen/Qwen2.5-1.5B-Instruct";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
