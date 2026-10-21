package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.api.MealPortion;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator.FoodShare;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MealPortionCalculatorTest {
    private final MealPortionCalculator calculator = new MealPortionCalculator();
    private final UUID food = UUID.randomUUID();
    private final UUID other = UUID.randomUUID();

    @Test
    void splitsTheDailyAmountOverTheMeals() {
        assertThat(this.calculator.calculate(List.of(new FoodShare(this.food, 120)), 4).portions())
                .containsExactly(new MealPortion(this.food, 30));
        assertThat(this.calculator.calculate(List.of(new FoodShare(this.food, 100)), 4).portions())
                .containsExactly(new MealPortion(this.food, 25));
    }

    @Test
    void roundsHalfUp() {
        assertThat(this.calculator.calculate(List.of(new FoodShare(this.food, 90)), 4).portions().get(0).grams()).isEqualTo(23);
        assertThat(MealPortionCalculator.perMealGrams(10, 3)).isEqualTo(3);
    }

    @Test
    void withoutGramsThereAreOnlyParts() {
        Result result = this.calculator.calculate(List.of(new FoodShare(this.food, null)), 4);

        assertThat(result.state()).isEqualTo(PortionState.PARTS);
        assertThat(result.portions()).containsExactly(new MealPortion(this.food, null));
    }

    @Test
    void feedingBothGivesEachFoodItsOwnPortion() {
        Result result = this.calculator.calculate(List.of(new FoodShare(this.food, 60), new FoodShare(this.other, 80)), 4);

        assertThat(result.state()).isEqualTo(PortionState.USUAL);
        assertThat(result.portions()).containsExactly(new MealPortion(this.food, 15), new MealPortion(this.other, 20));
    }

    @Test
    void oneFoodWithoutGramsMakesTheWholeMealParts() {
        Result result = this.calculator.calculate(List.of(new FoodShare(this.food, 60), new FoodShare(this.other, null)), 4);

        assertThat(result.state()).isEqualTo(PortionState.PARTS);
        assertThat(result.portions()).extracting(MealPortion::grams).containsOnlyNulls();
    }

    @Test
    void noGramsNoPerMealAmount() {
        assertThat(MealPortionCalculator.perMealGrams(null, 4)).isNull();
    }
}
