package com.picvi.pet_companion_service.development.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressStateTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);

    @Test
    void stateWithoutAttemptTodayIsUnchanged() {
        ProgressState state = new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.UNSURE, 1,
                DAY.minusDays(3), DAY.minusDays(3), null, null);

        assertThat(state.asOfStartOf(DAY)).isEqualTo(state);
    }

    @Test
    void completionTodayIsRevertedToNotStarted() {
        ProgressState state = new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null);

        ProgressState before = state.asOfStartOf(DAY);

        assertThat(before.status()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(before.attemptDays()).isZero();
        assertThat(before.lastAttemptOn()).isNull();
        assertThat(before.lastOutcome()).isNull();
        assertThat(before.completedOn()).isNull();
    }

    @Test
    void completionTodayAfterEarlierAttemptsIsRevertedToInProgress() {
        ProgressState state = new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 2,
                DAY.minusDays(2), DAY, DAY, null);

        ProgressState before = state.asOfStartOf(DAY);

        assertThat(before.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(before.attemptDays()).isEqualTo(1);
        assertThat(before.firstAttemptOn()).isEqualTo(DAY.minusDays(2));
    }

    @Test
    void earlierCompletionSurvivesAnAttemptToday() {
        ProgressState state = new ProgressState(ProgressStatus.COMPLETED, Outcome.SCARED, 2,
                DAY.minusDays(2), DAY, DAY.minusDays(2), null);

        ProgressState before = state.asOfStartOf(DAY);

        assertThat(before.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(before.completedOn()).isEqualTo(DAY.minusDays(2));
        assertThat(before.attemptDays()).isEqualTo(1);
    }

    @Test
    void snoozeAndNotPossibleAreNeverReverted() {
        ProgressState snoozed = new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.UNSURE, 1,
                DAY, DAY, null, DAY.plusDays(3));
        ProgressState hidden = new ProgressState(ProgressStatus.NOT_POSSIBLE, Outcome.SCARED, 1, DAY, DAY, null, null);

        assertThat(snoozed.asOfStartOf(DAY).snoozedUntil()).isEqualTo(DAY.plusDays(3));
        assertThat(hidden.asOfStartOf(DAY).status()).isEqualTo(ProgressStatus.NOT_POSSIBLE);
    }
}
