package com.picvi.pet_companion_service.nutrition.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodAskPolicyTest {
    private final FoodAskPolicy policy = new FoodAskPolicy();

    @Test
    void asksUntilDismissed() {
        assertThat(this.policy.state(false, false, 0)).isEqualTo(PortionState.ASK);
    }

    @Test
    void staysQuietAfterDismissalAndAsksOnceMoreOnTheThirdMeal() {
        assertThat(this.policy.state(true, false, 0)).isEqualTo(PortionState.NONE);
        assertThat(this.policy.state(true, false, 2)).isEqualTo(PortionState.NONE);
        assertThat(this.policy.state(true, false, 3)).isEqualTo(PortionState.ASK);
    }

    @Test
    void neverAsksAfterTheSecondDismissal() {
        assertThat(this.policy.state(true, true, 50)).isEqualTo(PortionState.NONE);
    }
}
