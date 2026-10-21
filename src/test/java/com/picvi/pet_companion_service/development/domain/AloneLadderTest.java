package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.exception.development.InvalidOutcome;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AloneLadderTest {

    @Test
    void firstGoalIsFiveSeconds() {
        assertThat(AloneLadder.firstGoalSeconds()).isEqualTo(5);
    }

    @Test
    void calmMovesOneStepUp() {
        assertThat(AloneLadder.suggestNext(5, Outcome.CALM)).isEqualTo(10);
        assertThat(AloneLadder.suggestNext(45, Outcome.CALM)).isEqualTo(60);
    }

    @Test
    void unsureStaysOnTheSameStep() {
        assertThat(AloneLadder.suggestNext(30, Outcome.UNSURE)).isEqualTo(30);
    }

    @Test
    void upsetMovesOneStepDown() {
        assertThat(AloneLadder.suggestNext(60, Outcome.SCARED)).isEqualTo(45);
    }

    @Test
    void ladderStopsAtBothEnds() {
        assertThat(AloneLadder.suggestNext(5, Outcome.SCARED)).isEqualTo(5);
        assertThat(AloneLadder.suggestNext(3600, Outcome.CALM)).isEqualTo(3600);
    }

    @Test
    void secondsBetweenStepsCountAsTheStepBelow() {
        assertThat(AloneLadder.suggestNext(50, Outcome.CALM)).isEqualTo(60);
        assertThat(AloneLadder.suggestNext(3, Outcome.CALM)).isEqualTo(10);
    }

    @Test
    void onlyReactionsAreAccepted() {
        assertThatThrownBy(() -> AloneLadder.suggestNext(10, Outcome.PRACTISED)).isInstanceOf(InvalidOutcome.class);
    }

    @Test
    void everySessionIsPracticeAndCalmPastTheThresholdIsLearned() {
        assertThat(AloneLadder.progressOutcome("ALONE_FIRST_SECONDS", 30, Outcome.CALM)).isEqualTo(Outcome.PRACTISED);
        assertThat(AloneLadder.progressOutcome("ALONE_FIRST_SECONDS", 60, Outcome.UNSURE)).isEqualTo(Outcome.PRACTISED);
        assertThat(AloneLadder.progressOutcome("ALONE_FIRST_SECONDS", 60, Outcome.CALM)).isEqualTo(Outcome.LEARNED);
        assertThat(AloneLadder.progressOutcome("ALONE_BUILD_MINUTES", 1799, Outcome.CALM)).isEqualTo(Outcome.PRACTISED);
        assertThat(AloneLadder.progressOutcome("ALONE_BUILD_MINUTES", 1800, Outcome.CALM)).isEqualTo(Outcome.LEARNED);
    }

    @Test
    void onlyTheTwoAloneActivitiesAreTracked() {
        assertThat(AloneLadder.isAloneActivity("ALONE_FIRST_SECONDS")).isTrue();
        assertThat(AloneLadder.isAloneActivity("ALONE_BUILD_MINUTES")).isTrue();
        assertThat(AloneLadder.isAloneActivity("NAP_ROUTINE")).isFalse();
    }
}
