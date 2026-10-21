package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.exception.nutrition.FoodNotTriable;
import com.picvi.pet_companion_service.exception.nutrition.InvalidTriedFood;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TriedFoodRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private final TriedFoodRules rules = new TriedFoodRules();

    @Test
    void aSafeFoodCanBeMarkedTried() {
        assertThatCode(() -> this.rules.validate(SafetyLevel.YES, TODAY, "loved it", TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> this.rules.validate(SafetyLevel.LITTLE, TODAY.minusDays(3), null, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void aNeverFoodCannot() {
        assertThatThrownBy(() -> this.rules.validate(SafetyLevel.NEVER, TODAY, null, TODAY)).isInstanceOf(FoodNotTriable.class);
    }

    @Test
    void theDayCannotBeInTheFuture() {
        assertThatThrownBy(() -> this.rules.validate(SafetyLevel.YES, TODAY.plusDays(1), null, TODAY))
                .isInstanceOf(InvalidTriedFood.class);
    }

    @Test
    void theNoteIsCappedAtTwoHundredCharacters() {
        assertThatCode(() -> this.rules.validate(SafetyLevel.YES, TODAY, "a".repeat(200), TODAY)).doesNotThrowAnyException();
        assertThatThrownBy(() -> this.rules.validate(SafetyLevel.YES, TODAY, "a".repeat(201), TODAY))
                .isInstanceOf(InvalidTriedFood.class);
    }
}
