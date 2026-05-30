package com.tradeagent.sector;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class SectorKeywordProvider {

    private static final Map<String, List<String>> STRONG_KEYWORDS = Map.of(
            "SEMI", List.of("semiconductor", "chip", "chips", "gpu", "foundry", "nvidia", "amd", "tsmc", "intel", "asml", "micron"),
            "AIINF", List.of("artificial intelligence", "ai infrastructure", "data center", "datacenter", "gpu cluster", "machine learning", "generative ai", "openai", "cloud compute", "inference", "training cluster"),
            "EV", List.of("electric vehicle", "battery", "charging", "lithium", "tesla", "rivian", "ev battery", "battery materials", "battery cell"),
            "BIO", List.of("biotech", "biotechnology", "clinical trial", "fda", "pharma", "pharmaceutical", "vaccine", "oncology", "drug trial", "biopharma", "therapy"),
            "CLOUD", List.of("cloud computing", "saas", "enterprise software", "aws", "azure", "salesforce", "oracle cloud", "cloud platform", "software subscription", "enterprise cloud"),
            "ENERGY", List.of("env_oil", "oil_and_gas", "energy_and_extractives", "refineries", "nuclear_energy", "crude_oil", "natural_gas", "power_systems", "electricalgrid", "oil", "gas", "uranium", "grid")
    );

    private static final Map<String, List<String>> SUPPORT_KEYWORDS = Map.of(
            "SEMI", List.of("fab", "fabless", "wafer", "memory", "hbm", "logic chip", "chipmaker"),
            "AIINF", List.of("compute", "accelerator", "server", "hyperscaler", "infrastructure", "llm", "foundation model"),
            "EV", List.of("electric car", "ev", "autonomous", "charging network", "battery pack", "cathode", "anode"),
            "BIO", List.of("drug", "medicine", "healthcare", "biologics", "therapeutics", "diagnostics"),
            "CLOUD", List.of("cloud", "enterprise app", "software", "platform", "subscription", "devops"),
            "ENERGY", List.of("energy", "utility", "power", "pipeline", "lng", "solar", "wind", "nuclear", "commodity")
    );

    public List<String> getStrongKeywords(String sectorCode) {
        return STRONG_KEYWORDS.getOrDefault(sectorCode, List.of());
    }

    public List<String> getSupportKeywords(String sectorCode) {
        return SUPPORT_KEYWORDS.getOrDefault(sectorCode, List.of());
    }

    public Map<String, List<String>> getAllStrongKeywords() {
        return STRONG_KEYWORDS;
    }
}
