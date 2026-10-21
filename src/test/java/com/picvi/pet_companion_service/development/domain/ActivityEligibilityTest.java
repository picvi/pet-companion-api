package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityEligibilityTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);
    private static final int AGE_WEEKS = 12;

    private static final ActivityDefinition ACTIVITY = definition(VaccinationRequirement.NONE, null);
    private static final ActivityDefinition RECURRING = definition(VaccinationRequirement.NONE, 7);

    @Test
    void activityIsLockedUntilVaccinationRequirementIsMet() {
        ActivityDefinition needsFullSeries = definition(VaccinationRequirement.SERIES_COMPLETE, null);

        assertThat(ActivityEligibility.isLocked(needsFullSeries, VaccinationStatus.FIRST_DOSE)).isTrue();
        assertThat(ActivityEligibility.isLocked(needsFullSeries, VaccinationStatus.SERIES_COMPLETE)).isFalse();
        assertThat(ActivityEligibility.isLocked(ACTIVITY, VaccinationStatus.UNKNOWN)).isFalse();
    }

    @Test
    void unsureRestsOnTheDayAfterAttempt() {
        ProgressState state = attempted(Outcome.UNSURE, DAY);

        assertThat(ActivityEligibility.isResting(state, DAY.plusDays(1))).isTrue();
    }

    @Test
    void restingUntilIsTwoDaysAfterUnsureAndSevenAfterScared() {
        assertThat(ActivityEligibility.restingUntil(attempted(Outcome.UNSURE, DAY))).isEqualTo(DAY.plusDays(2));
        assertThat(ActivityEligibility.restingUntil(attempted(Outcome.SCARED, DAY))).isEqualTo(DAY.plusDays(7));
    }

    @Test
    void restingUntilIsNullWithoutAnAttemptOrForOtherOutcomes() {
        assertThat(ActivityEligibility.restingUntil(ProgressState.notStarted())).isNull();
        assertThat(ActivityEligibility.restingUntil(attempted(Outcome.CALM, DAY))).isNull();
    }

    @Test
    void unsureStopsRestingTwoDaysAfterAttempt() {
        ProgressState state = attempted(Outcome.UNSURE, DAY);

        assertThat(ActivityEligibility.isResting(state, DAY.plusDays(2))).isFalse();
    }

    @Test
    void scaredRestsSixDaysAfterAttempt() {
        ProgressState state = attempted(Outcome.SCARED, DAY);

        assertThat(ActivityEligibility.isResting(state, DAY.plusDays(6))).isTrue();
    }

    @Test
    void scaredStopsRestingSevenDaysAfterAttempt() {
        ProgressState state = attempted(Outcome.SCARED, DAY);

        assertThat(ActivityEligibility.isResting(state, DAY.plusDays(7))).isFalse();
    }

    @Test
    void calmAndPractisedOutcomesNeverRest() {
        assertThat(ActivityEligibility.isResting(attempted(Outcome.CALM, DAY), DAY)).isFalse();
        assertThat(ActivityEligibility.isResting(attempted(Outcome.PRACTISED, DAY), DAY)).isFalse();
    }

    @Test
    void neverAttemptedActivityIsNotResting() {
        assertThat(ActivityEligibility.isResting(ProgressState.notStarted(), DAY)).isFalse();
    }

    @Test
    void snoozedUntilDateIsExclusive() {
        ProgressState state = snoozedUntil(DAY.plusDays(3));

        assertThat(ActivityEligibility.isSnoozed(state, DAY.plusDays(2))).isTrue();
        assertThat(ActivityEligibility.isSnoozed(state, DAY.plusDays(3))).isFalse();
        assertThat(ActivityEligibility.isSnoozed(ProgressState.notStarted(), DAY)).isFalse();
    }

    @Test
    void recurringActivityIsDueWhenNeverDone() {
        assertThat(ActivityEligibility.isRecurringDue(RECURRING, ProgressState.notStarted(), DAY)).isTrue();
    }

    @Test
    void recurringActivityIsDueAfterItsInterval() {
        ProgressState state = attempted(Outcome.PRACTISED, DAY);

        assertThat(ActivityEligibility.isRecurringDue(RECURRING, state, DAY.plusDays(6))).isFalse();
        assertThat(ActivityEligibility.isRecurringDue(RECURRING, state, DAY.plusDays(7))).isTrue();
    }

    @Test
    void nonRecurringActivityIsNeverDue() {
        assertThat(ActivityEligibility.isRecurringDue(ACTIVITY, ProgressState.notStarted(), DAY)).isFalse();
    }

    @Test
    void freshActivityInAgeWindowIsSuggestable() {
        assertThat(suggestable(ACTIVITY, ProgressState.notStarted(), VaccinationStatus.UNKNOWN, AGE_WEEKS)).isTrue();
    }

    @Test
    void activityOutsideAgeWindowIsNotSuggestable() {
        assertThat(suggestable(ACTIVITY, ProgressState.notStarted(), VaccinationStatus.UNKNOWN, 7)).isFalse();
        assertThat(suggestable(ACTIVITY, ProgressState.notStarted(), VaccinationStatus.UNKNOWN, 16)).isFalse();
    }

    @Test
    void lockedActivityIsNotSuggestable() {
        ActivityDefinition locked = definition(VaccinationRequirement.FIRST_DOSE, null);

        assertThat(suggestable(locked, ProgressState.notStarted(), VaccinationStatus.NONE, AGE_WEEKS)).isFalse();
        assertThat(suggestable(locked, ProgressState.notStarted(), VaccinationStatus.FIRST_DOSE, AGE_WEEKS)).isTrue();
    }

    @Test
    void notPossibleAndCompletedActivitiesAreNotSuggestable() {
        ProgressState notPossible = new ProgressState(ProgressStatus.NOT_POSSIBLE, null, 0, null, null, null, null);
        ProgressState completed = new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null);

        assertThat(suggestable(ACTIVITY, notPossible, VaccinationStatus.UNKNOWN, AGE_WEEKS)).isFalse();
        assertThat(suggestable(ACTIVITY, completed, VaccinationStatus.UNKNOWN, AGE_WEEKS)).isFalse();
    }

    @Test
    void restingAndSnoozedActivitiesAreNotSuggestable() {
        assertThat(suggestable(ACTIVITY, attempted(Outcome.SCARED, DAY.minusDays(1)), VaccinationStatus.UNKNOWN, AGE_WEEKS)).isFalse();
        assertThat(suggestable(ACTIVITY, snoozedUntil(DAY.plusDays(1)), VaccinationStatus.UNKNOWN, AGE_WEEKS)).isFalse();
    }

    @Test
    void recurringActivityIsSuggestableOnlyWhenDue() {
        ProgressState doneYesterday = attempted(Outcome.PRACTISED, DAY.minusDays(1));
        ProgressState doneLastWeek = attempted(Outcome.PRACTISED, DAY.minusDays(7));

        assertThat(suggestable(RECURRING, doneYesterday, VaccinationStatus.UNKNOWN, AGE_WEEKS)).isFalse();
        assertThat(suggestable(RECURRING, doneLastWeek, VaccinationStatus.UNKNOWN, AGE_WEEKS)).isTrue();
    }

    private static boolean suggestable(ActivityDefinition activity, ProgressState state,
                                       VaccinationStatus vaccinationStatus, int ageWeeks) {
        return ActivityEligibility.isSuggestable(activity, state, vaccinationStatus, ageWeeks, DAY);
    }

    private static ProgressState attempted(Outcome outcome, LocalDate attemptedOn) {
        return new ProgressState(ProgressStatus.IN_PROGRESS, outcome, 1, attemptedOn, attemptedOn, null, null);
    }

    private static ProgressState snoozedUntil(LocalDate date) {
        return new ProgressState(ProgressStatus.NOT_STARTED, null, 0, null, null, null, date);
    }

    private static ActivityDefinition definition(VaccinationRequirement requirement, Integer repeatEveryDays) {
        return new ActivityDefinition("ANY", ActivityKind.SOCIALIZATION, ActivityCategory.PEOPLE,
                8, 16, 1, requirement, repeatEveryDays, 10);
    }
}
