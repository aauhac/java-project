package com.tradeagent.sector;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SectorKeywordProvider {

    private static final Map<String, List<String>> STRONG_KEYWORDS;
    private static final Map<String, List<String>> SUPPORT_KEYWORDS;

    static {
        Map<String, List<String>> strong = new LinkedHashMap<>();
        strong.put("SEMI", List.of(
                "semiconductor", "chip", "gpu", "foundry", "nvidia", "amd", "tsmc", "intel", "asml"
        ));
        strong.put("AIINF", List.of(
                "artificial intelligence", "data center", "datacenter", "ai infrastructure", "gpu cluster",
                "openai", "machine learning", "generative ai"
        ));
        strong.put("EV", List.of(
                "electric vehicle", "battery", "charging", "lithium", "tesla", "rivian", "byd"
        ));
        strong.put("BIO", List.of(
                "biotech", "biotechnology", "clinical trial", "fda", "pharma", "pharmaceutical", "vaccine", "oncology"
        ));
        strong.put("CLOUD", List.of(
                "cloud computing", "saas", "enterprise software", "aws", "azure", "salesforce", "oracle cloud"
        ));
        strong.put("ENERGY", List.of(
                "env_oil", "oil_and_gas", "energy_and_extractives", "refineries", "nuclear_energy",
                "crude_oil", "natural_gas", "power_systems", "electricalgrid"
        ));

        Map<String, List<String>> support = new LinkedHashMap<>();
        support.put("SEMI", List.of("fab", "fabless", "wafer", "memory", "hbm", "chipmaker"));
        support.put("AIINF", List.of("infrastructure", "accelerator", "training cluster", "inference", "llm"));
        support.put("EV", List.of("electric car", "battery pack", "charging network", "anode", "cathode"));
        support.put("BIO", List.of("drug", "therapeutics", "biopharma", "healthcare", "diagnostics"));
        support.put("CLOUD", List.of("enterprise app", "software subscription", "cloud platform", "devops"));
        support.put("ENERGY", List.of("energy", "oil", "gas", "pipeline", "lng", "uranium", "utility"));

        STRONG_KEYWORDS = Map.copyOf(strong);
        SUPPORT_KEYWORDS = Map.copyOf(support);
    }

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
