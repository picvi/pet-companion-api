package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VaccinationRequirementTest {

    @Test
    void noRequirementIsAlwaysMet() {
        for (VaccinationStatus status : VaccinationStatus.values()) {
            assertThat(VaccinationRequirement.NONE.isMetBy(status)).isTrue();
        }
    }

    @Test
    void firstDoseRequirementNeedsFirstDoseOrSeriesComplete() {
        assertThat(VaccinationRequirement.FIRST_DOSE.isMetBy(VaccinationStatus.UNKNOWN)).isFalse();
        assertThat(VaccinationRequirement.FIRST_DOSE.isMetBy(VaccinationStatus.NONE)).isFalse();
        assertThat(VaccinationRequirement.FIRST_DOSE.isMetBy(VaccinationStatus.FIRST_DOSE)).isTrue();
        assertThat(VaccinationRequirement.FIRST_DOSE.isMetBy(VaccinationStatus.SERIES_COMPLETE)).isTrue();
    }

    @Test
    void seriesCompleteRequirementNeedsSeriesComplete() {
        assertThat(VaccinationRequirement.SERIES_COMPLETE.isMetBy(VaccinationStatus.UNKNOWN)).isFalse();
        assertThat(VaccinationRequirement.SERIES_COMPLETE.isMetBy(VaccinationStatus.NONE)).isFalse();
        assertThat(VaccinationRequirement.SERIES_COMPLETE.isMetBy(VaccinationStatus.FIRST_DOSE)).isFalse();
        assertThat(VaccinationRequirement.SERIES_COMPLETE.isMetBy(VaccinationStatus.SERIES_COMPLETE)).isTrue();
    }
}
