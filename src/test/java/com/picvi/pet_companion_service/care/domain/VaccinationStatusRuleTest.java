package com.picvi.pet_companion_service.care.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VaccinationStatusRuleTest {
    private static final int SERIES_TOTAL = 3;

    private final VaccinationStatusRule rule = new VaccinationStatusRule();

    @Test
    void noDosesGiveNoStatus() {
        assertThat(this.rule.statusFor(0, SERIES_TOTAL)).isEmpty();
    }

    @Test
    void firstDoseGivesFirstDose() {
        assertThat(this.rule.statusFor(1, SERIES_TOTAL)).contains(VaccinationStatus.FIRST_DOSE);
    }

    @Test
    void doseBeforeLastSeriesDoseStaysFirstDose() {
        assertThat(this.rule.statusFor(2, SERIES_TOTAL)).contains(VaccinationStatus.FIRST_DOSE);
    }

    @Test
    void lastSeriesDoseCompletesSeries() {
        assertThat(this.rule.statusFor(3, SERIES_TOTAL)).contains(VaccinationStatus.SERIES_COMPLETE);
    }

    @Test
    void dosesBeyondSeriesKeepSeriesComplete() {
        assertThat(this.rule.statusFor(5, SERIES_TOTAL)).contains(VaccinationStatus.SERIES_COMPLETE);
    }
}
