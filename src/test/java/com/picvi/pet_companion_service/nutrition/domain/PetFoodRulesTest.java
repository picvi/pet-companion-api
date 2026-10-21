package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetFoodRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate BORN = LocalDate.of(2026, 8, 1);
    private final PetFoodRules rules = new PetFoodRules();

    @Test
    void acceptsAFoodWithOrWithoutGrams() {
        assertThatCode(() -> this.rules.validate(FoodKind.MAIN, 120, false, TODAY, TODAY, BORN)).doesNotThrowAnyException();
        assertThatCode(() -> this.rules.validate(FoodKind.MAIN, null, false, null, TODAY, BORN)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnAmountOutOfRange() {
        assertThatThrownBy(() -> this.rules.validate(FoodKind.MAIN, 0, false, null, TODAY, BORN)).isInstanceOf(InvalidPetFood.class);
        assertThatThrownBy(() -> this.rules.validate(FoodKind.MAIN, 10001, false, null, TODAY, BORN)).isInstanceOf(InvalidPetFood.class);
    }

    @Test
    void onlyATreatCanBeTrainingFood() {
        assertThatCode(() -> this.rules.validate(FoodKind.TREAT, null, true, null, TODAY, BORN)).doesNotThrowAnyException();
        assertThatThrownBy(() -> this.rules.validate(FoodKind.MAIN, null, true, null, TODAY, BORN)).isInstanceOf(InvalidPetFood.class);
    }

    @Test
    void theStartMustBeBetweenBirthAndToday() {
        assertThatThrownBy(() -> this.rules.validate(FoodKind.MAIN, null, false, TODAY.plusDays(1), TODAY, BORN)).isInstanceOf(InvalidPetFood.class);
        assertThatThrownBy(() -> this.rules.validate(FoodKind.MAIN, null, false, BORN.minusDays(1), TODAY, BORN)).isInstanceOf(InvalidPetFood.class);
    }
}
