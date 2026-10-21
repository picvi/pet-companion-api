package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.exception.rhythm.AwakeOutOfRange;
import com.picvi.pet_companion_service.exception.rhythm.InvalidDayTimes;
import com.picvi.pet_companion_service.exception.rhythm.MealOutsideDay;
import com.picvi.pet_companion_service.exception.rhythm.MealsOutOfRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RhythmSettingsValidatorTest {
    private static final List<Integer> MEALS = List.of(435, 720, 930, 1110);

    private final RhythmSettingsValidator validator = new RhythmSettingsValidator();

    @Test
    void acceptsAValidDay() {
        assertThatCode(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, MEALS, 60))).doesNotThrowAnyException();
    }

    @Test
    void rejectsBedBeforeWake() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(1350, 420, MEALS, null)))
                .isInstanceOf(InvalidDayTimes.class);
    }

    @Test
    void rejectsADayShorterThanTenHours() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(600, 1000, MEALS, null)))
                .isInstanceOf(InvalidDayTimes.class);
    }

    @Test
    void rejectsADayLongerThanTwentyHours() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(120, 1400, MEALS, null)))
                .isInstanceOf(InvalidDayTimes.class);
    }

    @Test
    void rejectsTooFewOrTooManyMeals() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, List.of(435), null)))
                .isInstanceOf(MealsOutOfRange.class);
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, List.of(435, 500, 560, 620, 680, 740, 800), null)))
                .isInstanceOf(MealsOutOfRange.class);
    }

    @Test
    void rejectsTheSameMealTimeTwice() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, List.of(435, 435, 930, 1110), null)))
                .isInstanceOf(MealsOutOfRange.class);
    }

    @Test
    void rejectsMealsBeforeWakingOrDuringTheEveningWindDown() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, List.of(400, 720, 930, 1110), null)))
                .isInstanceOf(MealOutsideDay.class);
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, List.of(435, 720, 930, 1310), null)))
                .isInstanceOf(MealOutsideDay.class);
    }

    @Test
    void rejectsAnAwakeWindowOutsideTheRange() {
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, MEALS, 20)))
                .isInstanceOf(AwakeOutOfRange.class);
        assertThatThrownBy(() -> this.validator.validate(ELEVEN_WEEKS, settings(420, 1350, MEALS, 200)))
                .isInstanceOf(AwakeOutOfRange.class);
    }

    private static DaySettings settings(int wake, int bed, List<Integer> meals, Integer awake) {
        return new DaySettings(wake, bed, meals, awake);
    }
}
