package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.time.LocalDate;

public final class ActivityEligibility {
    private static final int UNSURE_REST_DAYS = 2;
    private static final int SCARED_REST_DAYS = 7;

    private ActivityEligibility() {}

    public static boolean isLocked(ActivityDefinition activity, VaccinationStatus vaccinationStatus) {
        return !activity.vaccinationRequirement().isMetBy(vaccinationStatus);
    }

    public static boolean isResting(ProgressState state, LocalDate date) {
        LocalDate restingUntil = restingUntil(state);
        return restingUntil != null && date.isBefore(restingUntil);
    }

    public static LocalDate restingUntil(ProgressState state) {
        if (state.lastAttemptOn() == null || state.lastOutcome() == null) {
            return null;
        }
        return switch (state.lastOutcome()) {
            case UNSURE -> state.lastAttemptOn().plusDays(UNSURE_REST_DAYS);
            case SCARED -> state.lastAttemptOn().plusDays(SCARED_REST_DAYS);
            default -> null;
        };
    }

    public static boolean isSnoozed(ProgressState state, LocalDate date) {
        return state.snoozedUntil() != null && date.isBefore(state.snoozedUntil());
    }

    public static boolean isRecurringDue(ActivityDefinition activity, ProgressState state, LocalDate date) {
        return activity.isRecurring()
                && (state.lastAttemptOn() == null
                || !date.isBefore(state.lastAttemptOn().plusDays(activity.repeatEveryDays())));
    }

    public static boolean isSuggestable(ActivityDefinition activity, ProgressState state,
                                        VaccinationStatus vaccinationStatus, int ageWeeks, LocalDate date) {
        return activity.coversAgeWeeks(ageWeeks)
                && !isLocked(activity, vaccinationStatus)
                && state.status() != ProgressStatus.NOT_POSSIBLE
                && state.status() != ProgressStatus.COMPLETED
                && !isResting(state, date)
                && !isSnoozed(state, date)
                && (!activity.isRecurring() || isRecurringDue(activity, state, date));
    }
}
