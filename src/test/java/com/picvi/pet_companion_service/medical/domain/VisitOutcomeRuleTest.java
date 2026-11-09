package com.picvi.pet_companion_service.medical.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VisitOutcomeRuleTest {
    private final VisitOutcomeRule rule = new VisitOutcomeRule();

    @Test
    void aJustCheckVisitIsFine() {
        assertThat(this.rule.derive(true, false, false, false)).containsExactly(VisitOutcome.FINE);
    }

    @Test
    void treatmentTestsAndFollowUpAreEachReported() {
        assertThat(this.rule.derive(false, true, true, true))
                .containsExactlyInAnyOrder(VisitOutcome.TREATMENT, VisitOutcome.TESTS, VisitOutcome.BACK);
    }

    @Test
    void aVisitWithNothingHasNoOutcome() {
        assertThat(this.rule.derive(false, false, false, false)).isEmpty();
    }
}
