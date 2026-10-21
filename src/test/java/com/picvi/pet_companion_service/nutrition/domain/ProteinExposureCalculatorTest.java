package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.dto.ProteinExposureDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProteinExposureCalculatorTest {
    private final ProteinExposureCalculator calculator = new ProteinExposureCalculator();

    @Test
    void chickenAsMainFoodSalmonAsTreatsAndRiceAsACarb() {
        List<ProteinExposureDto> proteins = this.calculator.calculate(List.of(
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.MAIN, false, LocalDate.of(2026, 10, 1), null),
                new ExposureRow(IngredientCode.RICE, FoodKind.MAIN, false, LocalDate.of(2026, 10, 1), null),
                new ExposureRow(IngredientCode.SALMON, FoodKind.TREAT, false, LocalDate.of(2026, 10, 9), null)));

        assertThat(proteins).extracting(ProteinExposureDto::code).containsExactly(IngredientCode.CHICKEN, IngredientCode.SALMON, IngredientCode.RICE);
        assertThat(proteins.get(0).kinds()).containsExactly(FoodKind.MAIN);
        assertThat(proteins.get(0).firstOn()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(proteins.get(1).kinds()).containsExactly(FoodKind.TREAT);
        assertThat(proteins.get(1).firstOn()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(proteins.get(2).sourceKind()).isEqualTo(IngredientKind.CARB);
    }

    @Test
    void anEndedFoodHasALastDayTheDayBeforeItWasNoLongerGiven() {
        List<ProteinExposureDto> proteins = this.calculator.calculate(List.of(
                new ExposureRow(IngredientCode.BEEF, FoodKind.MAIN, false, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 9))));

        assertThat(proteins.get(0).lastOn()).isEqualTo(LocalDate.of(2026, 10, 8));
    }

    @Test
    void theSameProteinInTwoFoodsMergesIntoOneEntry() {
        List<ProteinExposureDto> proteins = this.calculator.calculate(List.of(
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.MAIN, false, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)),
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.TREAT, false, LocalDate.of(2026, 9, 20), null)));

        assertThat(proteins).singleElement().satisfies(p -> {
            assertThat(p.kinds()).containsExactly(FoodKind.MAIN, FoodKind.TREAT);
            assertThat(p.firstOn()).isEqualTo(LocalDate.of(2026, 9, 20));
            assertThat(p.lastOn()).isNull();
        });
    }

    @Test
    void hydrolysedOnlyWhenEveryExposureWas() {
        List<ProteinExposureDto> mixed = this.calculator.calculate(List.of(
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.MAIN, true, LocalDate.of(2026, 10, 1), null),
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.TREAT, false, LocalDate.of(2026, 10, 2), null)));
        List<ProteinExposureDto> only = this.calculator.calculate(List.of(
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.MAIN, true, LocalDate.of(2026, 10, 1), null)));

        assertThat(mixed.get(0).hydrolysed()).isFalse();
        assertThat(only.get(0).hydrolysed()).isTrue();
    }

    @Test
    void nothingEatenMeansNothingListed() {
        assertThat(this.calculator.calculate(List.of())).isEmpty();
    }
}
