package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.exception.nutrition.ProteinRequired;
import com.picvi.pet_companion_service.nutrition.domain.FoodProductRules.IngredientInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodProductRulesTest {
    private final FoodProductRules rules = new FoodProductRules();

    @Test
    void mainProteinGivesKnownProtein() {
        List<IngredientInput> ingredients = List.of(
                new IngredientInput(IngredientCode.SALMON, IngredientRole.MAIN),
                new IngredientInput(IngredientCode.RICE, IngredientRole.OTHER));

        assertThat(this.rules.validate(ingredients, false)).isEqualTo(ProteinStatus.KNOWN);
    }

    @Test
    void notSureWithoutMainProteinGivesUnknownProtein() {
        List<IngredientInput> ingredients = List.of(new IngredientInput(IngredientCode.RICE, IngredientRole.OTHER));

        assertThat(this.rules.validate(ingredients, true)).isEqualTo(ProteinStatus.UNKNOWN);
    }

    @Test
    void noProteinAndNoNotSureIsRejected() {
        assertThatThrownBy(() -> this.rules.validate(List.of(), false)).isInstanceOf(ProteinRequired.class);
        assertThatThrownBy(() -> this.rules.validate(
                List.of(new IngredientInput(IngredientCode.RICE, IngredientRole.OTHER)), false))
                .isInstanceOf(ProteinRequired.class);
    }

    @Test
    void notSureTogetherWithMainProteinIsRejected() {
        assertThatThrownBy(() -> this.rules.validate(
                List.of(new IngredientInput(IngredientCode.BEEF, IngredientRole.MAIN)), true))
                .isInstanceOf(InvalidFoodProduct.class);
    }

    @Test
    void aCarbCannotBeTheMainProtein() {
        assertThatThrownBy(() -> this.rules.validate(
                List.of(new IngredientInput(IngredientCode.RICE, IngredientRole.MAIN)), false))
                .isInstanceOf(InvalidFoodProduct.class);
    }

    @Test
    void anIngredientCanBeListedOnlyOnce() {
        assertThatThrownBy(() -> this.rules.validate(List.of(
                new IngredientInput(IngredientCode.BEEF, IngredientRole.MAIN),
                new IngredientInput(IngredientCode.BEEF, IngredientRole.OTHER)), false))
                .isInstanceOf(InvalidFoodProduct.class);
    }
}
