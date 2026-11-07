package com.picvi.pet_companion_service.rhythm.domain;

import org.junit.jupiter.api.Test;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS;
import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.TWENTY_WEEKS;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultDayProposerTest {
    private final DefaultDayProposer proposer = new DefaultDayProposer();

    @Test
    void proposesAUsualDay() {
        DaySettings settings = this.proposer.propose(ELEVEN_WEEKS);

        assertThat(settings.wakeMinute()).isEqualTo(7 * 60);
        assertThat(settings.bedMinute()).isEqualTo(22 * 60 + 30);
    }

    @Test
    void spreadsTheStandardNumberOfMealsFromBreakfastToAnEarlyDinner() {
        assertThat(this.proposer.propose(ELEVEN_WEEKS).mealMinutes()).containsExactly(435, 660, 885, 1110);
        assertThat(this.proposer.propose(TWENTY_WEEKS).mealMinutes()).containsExactly(435, 772, 1110);
    }

    @Test
    void changingTheMealCountKeepsHerWakeAndBedTimes() {
        DaySettings current = new DaySettings(6 * 60, 22 * 60, java.util.List.of(375, 700), 90);

        DaySettings six = this.proposer.withMealCount(current, 6);

        assertThat(six.wakeMinute()).isEqualTo(360);
        assertThat(six.bedMinute()).isEqualTo(1320);
        assertThat(six.customAwakeMinutes()).isEqualTo(90);
        assertThat(six.mealMinutes()).hasSize(6).isSorted().doesNotHaveDuplicates();
        assertThat(six.mealMinutes().get(0)).isEqualTo(375);
    }

    @Test
    void aSpreadMealCountAlwaysPassesValidation() {
        DaySettings current = this.proposer.propose(ELEVEN_WEEKS);

        for (int meals : java.util.List.of(4, 5, 6)) {
            new RhythmSettingsValidator().validate(ELEVEN_WEEKS, this.proposer.withMealCount(current, meals));
        }
    }

    @Test
    void proposedMealsAlwaysPassValidation() {
        RhythmSettingsValidator validator = new RhythmSettingsValidator();

        validator.validate(ELEVEN_WEEKS, this.proposer.propose(ELEVEN_WEEKS));
        validator.validate(TWENTY_WEEKS, this.proposer.propose(TWENTY_WEEKS));
    }
}
