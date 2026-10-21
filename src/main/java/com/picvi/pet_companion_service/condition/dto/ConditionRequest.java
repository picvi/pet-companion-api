package com.picvi.pet_companion_service.condition.dto;

import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Used to add and to change. {@code resolvedOn} is the day it stopped being a problem; {@code date} is the client's today. */
public record ConditionRequest(@NotNull ConditionKind kind,
                               IngredientCode ingredientCode,
                               ConditionCertainty certainty,
                               String text,
                               LocalDate since,
                               LocalDate resolvedOn,
                               @NotNull LocalDate date) {
}
