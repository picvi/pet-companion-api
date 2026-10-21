package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.IngredientKind;
import com.picvi.pet_companion_service.nutrition.entity.IngredientSource;

public record IngredientSourceDto(String code, IngredientKind kind, String parentCode, boolean allergenRelevant, int sortOrder) {

    public static IngredientSourceDto of(IngredientSource source) {
        return new IngredientSourceDto(source.getCode(), source.getKind(), source.getParentCode(),
                source.isAllergenRelevant(), source.getSortOrder());
    }
}
