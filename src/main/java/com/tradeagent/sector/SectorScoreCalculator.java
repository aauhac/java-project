package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class SectorScoreCalculator {

    private final NewsSignalAggregator newsSignalAggregator;
    private final PriceMomentumCalculator priceMomentumCalculator;

    public SectorScoreCalculator(NewsSignalAggregator newsSignalAggregator,
                                 PriceMomentumCalculator priceMomentumCalculator) {
        this.newsSignalAggregator = newsSignalAggregator;
        this.priceMomentumCalculator = priceMomentumCalculator;
    }

    public SectorScore calculate(String sectorCode, LocalDate date) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();
        BigDecimal newsVolume = newsSignalAggregator.calculateNewsVolumeScore(sectorCode, resolvedDate);
        BigDecimal newsTone = newsSignalAggregator.calculateNewsToneScore(sectorCode, resolvedDate);
        BigDecimal priceMomentum = priceMomentumCalculator.calculatePriceMomentumScore(sectorCode, resolvedDate);
        BigDecimal volumeSpike = priceMomentumCalculator.calculateVolumeSpikeScore(sectorCode, resolvedDate);
        BigDecimal breadth = priceMomentumCalculator.calculateBreadthScore(sectorCode, resolvedDate);

        BigDecimal total = newsVolume.multiply(BigDecimal.valueOf(0.25))
                .add(newsTone.multiply(BigDecimal.valueOf(0.15)))
                .add(priceMomentum.multiply(BigDecimal.valueOf(0.25)))
                .add(volumeSpike.multiply(BigDecimal.valueOf(0.20)))
                .add(breadth.multiply(BigDecimal.valueOf(0.15)))
                .setScale(2, RoundingMode.HALF_UP);

        return new SectorScore(
                sectorCode,
                resolvedDate,
                newsVolume,
                newsTone,
                priceMomentum,
                volumeSpike,
                breadth,
                total
        );
    }
}
