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
        BigDecimal keywordStrength = priceMomentumCalculator.calculateBreadthScore(sectorCode, resolvedDate);
        BigDecimal total = newsVolume.multiply(BigDecimal.valueOf(0.5))
                .add(newsTone.multiply(BigDecimal.valueOf(0.3)))
                .add(keywordStrength.multiply(BigDecimal.valueOf(0.2)))
                .setScale(2, RoundingMode.HALF_UP);

        return new SectorScore(
                sectorCode,
                resolvedDate,
                0,
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                newsVolume.setScale(2, RoundingMode.HALF_UP),
                newsTone.setScale(2, RoundingMode.HALF_UP),
                keywordStrength.setScale(2, RoundingMode.HALF_UP),
                total,
                "NEUTRAL",
                DateTimeUtil.nowUtc()
        );
    }
}
