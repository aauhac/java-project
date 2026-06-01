package com.tradeagent.sector.gdelt;

import com.tradeagent.sector.gdelt.dto.GdeltRawFileRef;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class GdeltGkgFileSelector {

    public List<GdeltRawFileRef> selectDailySamples(List<GdeltRawFileRef> refs,
                                                    LocalDate startDate,
                                                    int days,
                                                    LocalTime sampleTime) {
        if (refs == null || refs.isEmpty() || startDate == null) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        Map<LocalDate, List<GdeltRawFileRef>> refsByDate = refs.stream()
                .collect(Collectors.groupingBy(ref -> ref.timestamp().toLocalDate()));

        List<GdeltRawFileRef> selected = new ArrayList<>();
        for (int offset = 0; offset < days; offset++) {
            LocalDate date = startDate.plusDays(offset);
            if (date.isAfter(today) || date.equals(today)) {
                continue;
            }
            List<GdeltRawFileRef> dailyRefs = refsByDate.getOrDefault(date, List.of());
            if (dailyRefs.isEmpty()) {
                continue;
            }
            LocalDateTime target = LocalDateTime.of(date, sampleTime);
            GdeltRawFileRef nearest = dailyRefs.stream()
                    .min(Comparator.comparingLong(ref -> 
                        Math.abs(java.time.Duration.between(target, ref.timestamp()).toSeconds())))
                    .orElse(null);
            if (nearest != null) {
                selected.add(nearest);
            }
        }
        return selected.stream().distinct().sorted(Comparator.comparing(GdeltRawFileRef::timestamp)).toList();
    }
}