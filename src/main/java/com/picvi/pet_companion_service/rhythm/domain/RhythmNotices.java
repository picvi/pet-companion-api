package com.picvi.pet_companion_service.rhythm.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RhythmNotices {
    static final int CHECKIN_AFTER_DAYS = 14;

    public Integer bandNoteFromWeeks(Integer seenFromWeeks, RhythmRule rule) {
        return seenFromWeeks != null && seenFromWeeks != rule.fromWeeks() ? rule.fromWeeks() : null;
    }

    public boolean checkinDue(LocalDate settingsCreatedOn, LocalDate checkinAnsweredOn, LocalDate today) {
        return checkinAnsweredOn == null && ChronoUnit.DAYS.between(settingsCreatedOn, today) >= CHECKIN_AFTER_DAYS;
    }
}
