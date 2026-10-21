package com.picvi.pet_companion_service.care.domain;

import java.time.LocalDate;

public record CareItemSettings(boolean remindersEnabled, LocalDate pausedUntil, Integer intervalDays) {

    public static CareItemSettings defaultsFor(CareRequirement requirement) {
        return new CareItemSettings(requirement != CareRequirement.OPTIONAL, null, null);
    }
}
