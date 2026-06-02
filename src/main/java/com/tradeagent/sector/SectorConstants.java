package com.tradeagent.sector;

import java.util.List;
import java.util.Map;

public final class SectorConstants {

    public static final List<String> SUPPORTED_SECTORS =
            List.of("SEMI", "AIINF", "EV", "BIO", "CLOUD", "ENERGY");

    public static final Map<String, String> SECTOR_NAMES = Map.of(
            "SEMI", "반도체",
            "AIINF", "AI 인프라",
            "EV", "전기차",
            "BIO", "바이오",
            "CLOUD", "클라우드",
            "ENERGY", "에너지"
    );

    private SectorConstants() {
    }

    public static String nameOf(String sectorCode) {
        if (sectorCode == null) {
            return "";
        }
        return SECTOR_NAMES.getOrDefault(sectorCode.toUpperCase(), sectorCode);
    }
}