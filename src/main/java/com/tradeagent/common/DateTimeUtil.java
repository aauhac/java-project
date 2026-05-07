package com.tradeagent.common;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private DateTimeUtil() {}

    public static LocalDate today() {
        return LocalDate.now(UTC);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(UTC);
    }

    public static LocalDateTime nowKst() {
        return LocalDateTime.now(KST);
    }

    public static LocalDate tradingDaysBefore(LocalDate base, int days) {
        LocalDate result = base;
        int count = 0;
        while (count < days) {
            result = result.minusDays(1);
            DayOfWeek dow = result.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return result;
    }

    public static LocalDate tradingDaysAfter(LocalDate base, int days) {
        LocalDate result = base;
        int count = 0;
        while (count < days) {
            result = result.plusDays(1);
            DayOfWeek dow = result.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return result;
    }

    public static String formatDate(LocalDate date) {
        return date.format(ISO_DATE);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(ISO_DATE_TIME);
    }
}
