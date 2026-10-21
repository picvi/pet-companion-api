package com.picvi.pet_companion_service.development.domain;

import java.time.LocalDate;

public record ProgressState(ProgressStatus status, Outcome lastOutcome, int attemptDays,
                            LocalDate firstAttemptOn, LocalDate lastAttemptOn,
                            LocalDate completedOn, LocalDate snoozedUntil) {

    public static ProgressState notStarted() {
        return new ProgressState(ProgressStatus.NOT_STARTED, null, 0, null, null, null, null);
    }

    public ProgressState asOfStartOf(LocalDate date) {
        if (!date.equals(lastAttemptOn)) {
            return this;
        }

        int attemptDaysBefore = attemptDays - 1;
        boolean completedToday = date.equals(completedOn);
        ProgressStatus statusBefore = !completedToday ? status
                : attemptDaysBefore > 0 ? ProgressStatus.IN_PROGRESS : ProgressStatus.NOT_STARTED;

        return new ProgressState(statusBefore, null, attemptDaysBefore, attemptDaysBefore > 0 ? firstAttemptOn : null,
                null, completedToday ? null : completedOn, snoozedUntil);
    }
}
