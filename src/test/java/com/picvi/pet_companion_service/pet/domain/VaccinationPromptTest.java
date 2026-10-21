package com.picvi.pet_companion_service.pet.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VaccinationPromptTest {
    private final VaccinationPrompt prompt = new VaccinationPrompt();

    @Test
    void firstPromptIsDueAtSixteenWeeks() {
        assertThat(prompt.isDue(VaccinationStatus.UNKNOWN, 0, 16)).isTrue();
    }

    @Test
    void secondPromptIsDueAtEighteenWeeks() {
        assertThat(prompt.isDue(VaccinationStatus.NONE, 1, 18)).isTrue();
    }

    @Test
    void thirdPromptIsDueAtTwentyWeeks() {
        assertThat(prompt.isDue(VaccinationStatus.FIRST_DOSE, 2, 20)).isTrue();
    }

    @Test
    void notDueBeforeThreshold() {
        assertThat(prompt.isDue(VaccinationStatus.UNKNOWN, 0, 15)).isFalse();
        assertThat(prompt.isDue(VaccinationStatus.UNKNOWN, 1, 17)).isFalse();
        assertThat(prompt.isDue(VaccinationStatus.UNKNOWN, 2, 19)).isFalse();
    }

    @Test
    void neverDueAfterThreePrompts() {
        assertThat(prompt.isDue(VaccinationStatus.UNKNOWN, 3, 60)).isFalse();
    }

    @Test
    void neverDueWhenSeriesIsComplete() {
        assertThat(prompt.isDue(VaccinationStatus.SERIES_COMPLETE, 0, 30)).isFalse();
    }
}
