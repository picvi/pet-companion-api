package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import jakarta.validation.constraints.NotNull;

public record IngredientDto(@NotNull IngredientCode code, @NotNull IngredientRole role, Boolean hydrolysed) {

    public IngredientDto {
        if (hydrolysed == null) {
            hydrolysed = false;
        }
    }

    public static IngredientDto of(FoodProductIngredient ingredient) {
        return new IngredientDto(IngredientCode.valueOf(ingredient.getSourceCode()), ingredient.getRole(), ingredient.isHydrolysed());
    }
}
