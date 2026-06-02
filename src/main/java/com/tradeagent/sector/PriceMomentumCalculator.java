package com.tradeagent.sector;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class PriceMomentumCalculator {

    public PriceMomentumCalculator() {
    }

    public BigDecimal calculatePriceMomentumScore(String sectorCode, LocalDate date) {
        return neutralScore();
    }

    public BigDecimal calculateVolumeSpikeScore(String sectorCode, LocalDate date) {
        return neutralScore();
    }

    public BigDecimal calculateBreadthScore(String sectorCode, LocalDate date) {
        return neutralScore();
    }

    private BigDecimal neutralScore() {
        return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
    }
}
