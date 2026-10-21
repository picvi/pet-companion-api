package com.picvi.pet_companion_service.brief.domain;

import java.time.LocalDate;
import java.util.Map;

public class TrendCalculator {

    public Trend calculate(Map<LocalDate, Integer> dailyCounts, LocalDate to) {
        int window = BriefLimits.TREND_WINDOW_DAYS;
        int recent = this.sum(dailyCounts, to.minusDays(window - 1L), to);
        int earlier = this.sum(dailyCounts, to.minusDays(2L * window - 1), to.minusDays(window));
        if (recent == 0 && earlier == 0) {
            return Trend.NONE;
        }
        if (earlier == 0) {
            return Trend.NEW;
        }
        if (recent > earlier) {
            return Trend.WORSENING;
        }
        return recent == earlier ? Trend.STEADY : Trend.IMPROVING;
    }

    private int sum(Map<LocalDate, Integer> dailyCounts, LocalDate from, LocalDate to) {
        int total = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            total += dailyCounts.getOrDefault(day, 0);
        }
        return total;
    }
}
