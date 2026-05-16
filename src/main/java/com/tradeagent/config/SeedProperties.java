package com.tradeagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "trade.seed")
public class SeedProperties {
    private boolean enabled = true;
    private boolean referenceDataEnabled = true;
    private boolean sampleDataEnabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isReferenceDataEnabled() {
        return referenceDataEnabled;
    }

    public void setReferenceDataEnabled(boolean referenceDataEnabled) {
        this.referenceDataEnabled = referenceDataEnabled;
    }

    public boolean isSampleDataEnabled() {
        return sampleDataEnabled;
    }

    public void setSampleDataEnabled(boolean sampleDataEnabled) {
        this.sampleDataEnabled = sampleDataEnabled;
    }
}
