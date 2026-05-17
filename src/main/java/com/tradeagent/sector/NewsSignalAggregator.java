package com.tradeagent.sector;

import com.tradeagent.common.DateTimeUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@Transactional(readOnly = true)
public class NewsSignalAggregator {

    private final NewsEventRepository newsEventRepository;

    public NewsSignalAggregator(NewsEventRepository newsEventRepository) {
        this.newsEventRepository = newsEventRepository;
    }

    @Transactional
    public BigDecimal calculateNewsVolumeScore(String sectorCode, LocalDate date) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();
        List<NewsEvent> todayEvents = getNewsEvents(sectorCode, resolvedDate);

        BigDecimal currentCount = BigDecimal.valueOf(todayEvents.size());
        BigDecimal historicalAverage = historicalDailyAverage(sectorCode, resolvedDate, 7);
        if (historicalAverage.compareTo(BigDecimal.ZERO) <= 0) {
            if (currentCount.compareTo(BigDecimal.valueOf(8)) >= 0) {
                return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
            }
            if (currentCount.compareTo(BigDecimal.valueOf(4)) >= 0) {
                return BigDecimal.valueOf(75).setScale(2, RoundingMode.HALF_UP);
            }
            if (currentCount.compareTo(BigDecimal.ZERO) > 0) {
                return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
            }
            return BigDecimal.valueOf(40).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal ratio = currentCount.divide(historicalAverage, 6, RoundingMode.HALF_UP);
        BigDecimal score = ratio.multiply(BigDecimal.valueOf(50));
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public BigDecimal calculateNewsToneScore(String sectorCode, LocalDate date) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();
        List<NewsEvent> events = getNewsEvents(sectorCode, resolvedDate);
        if (events.isEmpty()) {
            return BigDecimal.valueOf(50).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal averageTone = events.stream()
                .map(NewsEvent::getToneScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(events.size()), 6, RoundingMode.HALF_UP);

        BigDecimal score = BigDecimal.valueOf(50).add(averageTone.multiply(BigDecimal.valueOf(8)));
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public List<NewsEvent> getNewsEvents(String sectorCode, LocalDate date) {
        LocalDate resolvedDate = date != null ? date : DateTimeUtil.today();
        LocalDateTime start = resolvedDate.atStartOfDay();
        LocalDateTime end = resolvedDate.atTime(LocalTime.MAX);
        return newsEventRepository.findBySectorCodeAndPublishedAtBetweenOrderByPublishedAtDesc(sectorCode, start, end);
    }

    private BigDecimal historicalDailyAverage(String sectorCode, LocalDate date, int lookbackDays) {
        if (lookbackDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        int countedDays = 0;
        for (int i = 1; i <= lookbackDays; i++) {
            LocalDate targetDate = date.minusDays(i);
            List<NewsEvent> events = newsEventRepository.findBySectorCodeAndPublishedAtBetweenOrderByPublishedAtDesc(
                    sectorCode,
                    targetDate.atStartOfDay(),
                    targetDate.atTime(LocalTime.MAX)
            );
            if (!events.isEmpty()) {
                total = total.add(BigDecimal.valueOf(events.size()));
                countedDays++;
            }
        }

        if (countedDays == 0) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(countedDays), 6, RoundingMode.HALF_UP);
    }
}
