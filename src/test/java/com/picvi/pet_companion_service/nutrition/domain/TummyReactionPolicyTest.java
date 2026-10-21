package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.domain.ReactionSignal.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TummyReactionPolicyTest {
    private final TummyReactionPolicy policy = new TummyReactionPolicy();

    private static ReactionSignal signal(Kind kind) {
        return new ReactionSignal(UUID.randomUUID(), kind);
    }

    @Test
    void nothingHappenedMeansNothingToDo() {
        assertThat(this.policy.evaluate(List.of(), List.of()).kind()).isEqualTo(ReactionKind.NONE);
        assertThat(this.policy.evaluate(List.of(signal(Kind.MEAL_EATEN)), List.of(signal(Kind.MEAL_EATEN))).kind()).isEqualTo(ReactionKind.NONE);
    }

    @Test
    void theFirstLooseStoolOfAStepSlowsTheStepDown() {
        ReactionSignal loose = signal(Kind.LOOSE_STOOL);

        TummyReaction reaction = this.policy.evaluate(List.of(loose), List.of());

        assertThat(reaction.kind()).isEqualTo(ReactionKind.EXTEND_STEP);
        assertThat(reaction.logId()).isEqualTo(loose.logId());
    }

    @Test
    void aSecondLooseStoolInTheSameStepOnlySuggestsAPause() {
        ReactionSignal second = signal(Kind.LOOSE_STOOL);

        TummyReaction reaction = this.policy.evaluate(List.of(signal(Kind.LOOSE_STOOL), second), List.of());

        assertThat(reaction.kind()).isEqualTo(ReactionKind.SUGGEST_PAUSE);
        assertThat(reaction.logId()).isEqualTo(second.logId());
    }

    @Test
    void vomitingBloodOrAVeryUnwellDogPausesAndSendsToTheVet() {
        for (Kind kind : List.of(Kind.VOMITING, Kind.BLOOD, Kind.VERY_UNWELL)) {
            assertThat(this.policy.evaluate(List.of(signal(Kind.LOOSE_STOOL), signal(kind)), List.of()).kind())
                    .isEqualTo(ReactionKind.PAUSE_AND_VET);
        }
    }

    @Test
    void twoRefusedMealsInARowPauseTheChange() {
        ReactionSignal newest = signal(Kind.MEAL_REFUSED);

        TummyReaction reaction = this.policy.evaluate(List.of(), List.of(newest, signal(Kind.MEAL_REFUSED)));

        assertThat(reaction.kind()).isEqualTo(ReactionKind.PAUSE_AND_VET);
        assertThat(reaction.logId()).isEqualTo(newest.logId());
    }

    @Test
    void oneRefusedMealOrARefusalFollowedByAnEatenMealIsNotEnough() {
        assertThat(this.policy.evaluate(List.of(), List.of(signal(Kind.MEAL_REFUSED))).kind()).isEqualTo(ReactionKind.NONE);
        assertThat(this.policy.evaluate(List.of(), List.of(signal(Kind.MEAL_EATEN), signal(Kind.MEAL_REFUSED))).kind()).isEqualTo(ReactionKind.NONE);
    }
}
