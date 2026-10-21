package com.picvi.pet_companion_service.condition.dto;

import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionKind;
import com.picvi.pet_companion_service.condition.entity.PetCondition;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;

import java.time.LocalDate;
import java.util.UUID;

public record ConditionDto(UUID id,
                           ConditionKind kind,
                           IngredientCode ingredientCode,
                           ConditionCertainty certainty,
                           String text,
                           LocalDate since,
                           LocalDate resolvedOn,
                           long version) {

    public static ConditionDto of(PetCondition condition) {
        return new ConditionDto(condition.getId(), condition.getKind(),
                condition.getIngredientCode() == null ? null : IngredientCode.valueOf(condition.getIngredientCode()),
                condition.getCertainty(), condition.getText(), condition.getSince(), condition.getResolvedOn(), condition.getVersion());
    }
}
