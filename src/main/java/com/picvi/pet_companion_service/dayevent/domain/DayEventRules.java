package com.picvi.pet_companion_service.dayevent.domain;

import com.picvi.pet_companion_service.exception.dayevent.InvalidDayEvent;

import java.time.LocalDate;

public class DayEventRules {
    static final int MIN_DURATION_MINUTES = 15;
    static final int MAX_DURATION_MINUTES = 720;
    static final int MAX_TITLE_LENGTH = 60;

    public void validate(int startMinute, int durationMinutes, String title, LocalDate firstDate, LocalDate today) {
        boolean valid = startMinute >= 0
                && durationMinutes >= MIN_DURATION_MINUTES
                && durationMinutes <= MAX_DURATION_MINUTES
                && startMinute + durationMinutes <= 1440
                && (title == null || title.length() <= MAX_TITLE_LENGTH)
                && !firstDate.isBefore(today);
        if (!valid) {
            throw new InvalidDayEvent();
        }
    }

    public void validateStart(int startMinute, int durationMinutes) {
        if (startMinute < 0 || startMinute + durationMinutes > 1440) {
            throw new InvalidDayEvent();
        }
    }
}
