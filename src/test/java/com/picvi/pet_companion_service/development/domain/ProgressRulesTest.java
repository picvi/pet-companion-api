package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.exception.development.ActivityAlreadyCompleted;
import com.picvi.pet_companion_service.exception.development.InvalidOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgressRulesTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);

    private static final ActivityDefinition SOCIALISATION_ACTIVITY = new ActivityDefinition(
            "PEOPLE_CALM_VISITOR", ActivityKind.SOCIALIZATION, ActivityCategory.PEOPLE,
            8, 16, 1, VaccinationRequirement.NONE, null, 10);
    private static final ActivityDefinition SKILL_ACTIVITY = new ActivityDefinition(
            "SIT", ActivityKind.SKILL, ActivityCategory.FOUNDATION_CUES,
            8, 52, 1, VaccinationRequirement.NONE, null, 10);
    private static final ActivityDefinition RECURRING_ACTIVITY = new ActivityDefinition(
            "WEEKLY_WEIGH_IN", ActivityKind.SKILL, ActivityCategory.CARE,
            8, 52, 1, VaccinationRequirement.NONE, 7, 10);

    private final ProgressRules rules = new ProgressRules();

    @Test
    void calmCompletesSocialisationActivity() {
        ProgressState state = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.CALM, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(state.completedOn()).isEqualTo(DAY);
        assertThat(state.lastOutcome()).isEqualTo(Outcome.CALM);
        assertThat(state.attemptDays()).isEqualTo(1);
        assertThat(state.firstAttemptOn()).isEqualTo(DAY);
        assertThat(state.lastAttemptOn()).isEqualTo(DAY);
    }

    @Test
    void unsureKeepsInProgress() {
        ProgressState state = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(state.completedOn()).isNull();
    }

    @Test
    void scaredKeepsInProgress() {
        ProgressState state = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.SCARED, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(state.lastOutcome()).isEqualTo(Outcome.SCARED);
    }

    @Test
    void learnedCompletesSkill() {
        ProgressState state = rules.applyOutcome(ProgressState.notStarted(), SKILL_ACTIVITY, Outcome.LEARNED, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(state.completedOn()).isEqualTo(DAY);
    }

    @Test
    void practisedKeepsSkillInProgress() {
        ProgressState state = rules.applyOutcome(ProgressState.notStarted(), SKILL_ACTIVITY, Outcome.PRACTISED, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void socialisationOutcomeOnSkillIsRejected() {
        assertThatThrownBy(() -> rules.applyOutcome(ProgressState.notStarted(), SKILL_ACTIVITY, Outcome.CALM, DAY))
                .isInstanceOf(InvalidOutcome.class);
    }

    @Test
    void skillOutcomeOnSocialisationIsRejected() {
        assertThatThrownBy(() -> rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.LEARNED, DAY))
                .isInstanceOf(InvalidOutcome.class);
    }

    @Test
    void sameDayAttemptDoesNotIncreaseAttemptDays() {
        ProgressState first = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY);

        ProgressState second = rules.applyOutcome(first, SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY);

        assertThat(second.attemptDays()).isEqualTo(1);
    }

    @Test
    void nextDayAttemptIncreasesAttemptDays() {
        ProgressState first = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY);

        ProgressState second = rules.applyOutcome(first, SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY.plusDays(1));

        assertThat(second.attemptDays()).isEqualTo(2);
        assertThat(second.firstAttemptOn()).isEqualTo(DAY);
        assertThat(second.lastAttemptOn()).isEqualTo(DAY.plusDays(1));
    }

    @Test
    void sameDayCorrectionFromCalmToUnsureReopens() {
        ProgressState completed = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.CALM, DAY);

        ProgressState corrected = rules.applyOutcome(completed, SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY);

        assertThat(corrected.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(corrected.completedOn()).isNull();
        assertThat(corrected.attemptDays()).isEqualTo(1);
    }

    @Test
    void laterScaredDoesNotUncompleteEarlierCompletion() {
        ProgressState completed = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.CALM, DAY);

        ProgressState later = rules.applyOutcome(completed, SOCIALISATION_ACTIVITY, Outcome.SCARED, DAY.plusDays(1));

        assertThat(later.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(later.completedOn()).isEqualTo(DAY);
        assertThat(later.lastOutcome()).isEqualTo(Outcome.SCARED);
        assertThat(later.attemptDays()).isEqualTo(2);
    }

    @Test
    void recurringActivityNeverCompletes() {
        ProgressState state = rules.applyOutcome(ProgressState.notStarted(), RECURRING_ACTIVITY, Outcome.LEARNED, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(state.completedOn()).isNull();
    }

    @Test
    void outcomeClearsSnooze() {
        ProgressState snoozed = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_TODAY, DAY);

        ProgressState state = rules.applyOutcome(snoozed, SOCIALISATION_ACTIVITY, Outcome.UNSURE, DAY);

        assertThat(state.snoozedUntil()).isNull();
    }

    @Test
    void notTodaySnoozesForThreeDays() {
        ProgressState state = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_TODAY, DAY);

        assertThat(state.snoozedUntil()).isEqualTo(DAY.plusDays(3));
        assertThat(state.status()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    void notPossibleHidesActivity() {
        ProgressState state = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_POSSIBLE, DAY);

        assertThat(state.status()).isEqualTo(ProgressStatus.NOT_POSSIBLE);
        assertThat(state.snoozedUntil()).isNull();
    }

    @Test
    void skippingCompletedActivityIsRejected() {
        ProgressState completed = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.CALM, DAY);

        assertThatThrownBy(() -> rules.applySkip(completed, SkipReason.NOT_TODAY, DAY.plusDays(1)))
                .isInstanceOf(ActivityAlreadyCompleted.class);
    }

    @Test
    void restoreWithoutAttemptsReturnsNotStarted() {
        ProgressState hidden = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_POSSIBLE, DAY);

        ProgressState restored = rules.restore(hidden);

        assertThat(restored.status()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(restored.snoozedUntil()).isNull();
    }

    @Test
    void restoreWithAttemptsReturnsInProgress() {
        ProgressState attempted = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.SCARED, DAY);
        ProgressState hidden = rules.applySkip(attempted, SkipReason.NOT_POSSIBLE, DAY.plusDays(1));

        ProgressState restored = rules.restore(hidden);

        assertThat(restored.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void restoreKeepsCompletedActivityCompleted() {
        ProgressState completed = rules.applyOutcome(ProgressState.notStarted(), SOCIALISATION_ACTIVITY, Outcome.CALM, DAY);

        ProgressState restored = rules.restore(completed);

        assertThat(restored.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(restored.completedOn()).isEqualTo(DAY);
    }
}
