package com.picvi.pet_companion_service.medical.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/** Cuts newest-first rows into pages that never split a day, so a date is enough to ask for the next page. */
public class VisitHistoryPaging {
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public record Page<T>(List<T> rows, LocalDate nextBefore) {
    }

    public int size(Integer requested) {
        if (requested == null) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(requested, MAX_SIZE));
    }

    /** {@code rows} holds up to one row more than {@code size}; the extra row only tells that more exist. */
    public <T> Page<T> cut(List<T> rows, int size, Function<T, LocalDate> dayOf) {
        if (rows.size() <= size) {
            return new Page<>(rows, null);
        }
        LocalDate firstDayLeft = dayOf.apply(rows.get(size));
        List<T> wholeDays = rows.stream().takeWhile(row -> dayOf.apply(row).isAfter(firstDayLeft)).toList();
        if (wholeDays.isEmpty()) {
            List<T> page = rows.subList(0, size);
            return new Page<>(page, dayOf.apply(page.getLast()));
        }
        return new Page<>(wholeDays, firstDayLeft.plusDays(1));
    }
}
