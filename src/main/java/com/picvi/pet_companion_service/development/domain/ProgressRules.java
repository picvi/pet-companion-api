package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.exception.development.ActivityAlreadyCompleted;
import com.picvi.pet_companion_service.exception.development.InvalidOutcome;

import java.time.LocalDate;
import java.util.Set;

public class ProgressRules {
    private static final int NOT_TODAY_SNOOZE_DAYS = 3;
    private static final Set<Outcome> SOCIALIZATION_OUTCOMES = Set.of(Outcome.CALM, Outcome.UNSURE, Outcome.SCARED);
    private static final Set<Outcome> SKILL_OUTCOMES = Set.of(Outcome.PRACTISED, Outcome.LEARNED);
    private static final Set<Outcome> COMPLETING_OUTCOMES = Set.of(Outcome.CALM, Outcome.LEARNED);

    public ProgressState applyOutcome(ProgressState current, ActivityDefinition activity, Outcome outcome, LocalDate date) {
        requireOutcomeFitsKind(activity.kind(), outcome);

        boolean firstAttemptOfTheDay = !date.equals(current.lastAttemptOn());
        int attemptDays = firstAttemptOfTheDay ? current.attemptDays() + 1 : current.attemptDays();
        LocalDate firstAttemptOn = current.firstAttemptOn() != null ? current.firstAttemptOn() : date;

        ProgressStatus status;
        LocalDate completedOn;
        if (activity.isRecurring()) {
            status = ProgressStatus.IN_PROGRESS;
            completedOn = null;
        } else if (COMPLETING_OUTCOMES.contains(outcome)) {
            status = ProgressStatus.COMPLETED;
            completedOn = current.completedOn() != null ? current.completedOn() : date;
        } else if (wasCompletedBefore(current, date)) {
            status = ProgressStatus.COMPLETED;
            completedOn = current.completedOn();
        } else {
            status = ProgressStatus.IN_PROGRESS;
            completedOn = null;
        }

        return new ProgressState(status, outcome, attemptDays, firstAttemptOn, date, completedOn, null);
    }

    public ProgressState applySkip(ProgressState current, SkipReason reason, LocalDate date) {
        if (current.status() == ProgressStatus.COMPLETED) {
            throw new ActivityAlreadyCompleted();
        }

        return switch (reason) {
            case NOT_TODAY -> withStatusAndSnooze(current, current.status(), date.plusDays(NOT_TODAY_SNOOZE_DAYS));
            case NOT_POSSIBLE -> withStatusAndSnooze(current, ProgressStatus.NOT_POSSIBLE, null);
        };
    }

    public ProgressState restore(ProgressState current) {
        if (current.status() == ProgressStatus.COMPLETED) {
            return withStatusAndSnooze(current, ProgressStatus.COMPLETED, null);
        }
        ProgressStatus status = current.attemptDays() > 0 ? ProgressStatus.IN_PROGRESS : ProgressStatus.NOT_STARTED;
        return withStatusAndSnooze(current, status, null);
    }

    private static void requireOutcomeFitsKind(ActivityKind kind, Outcome outcome) {
        Set<Outcome> allowedOutcomes = kind == ActivityKind.SOCIALIZATION ? SOCIALIZATION_OUTCOMES : SKILL_OUTCOMES;
        if (!allowedOutcomes.contains(outcome)) {
            throw new InvalidOutcome();
        }
    }

    private static boolean wasCompletedBefore(ProgressState current, LocalDate date) {
        return current.status() == ProgressStatus.COMPLETED && current.completedOn().isBefore(date);
    }

    private static ProgressState withStatusAndSnooze(ProgressState current, ProgressStatus status, LocalDate snoozedUntil) {
        return new ProgressState(status, current.lastOutcome(), current.attemptDays(), current.firstAttemptOn(),
                current.lastAttemptOn(), current.completedOn(), snoozedUntil);
    }
}
