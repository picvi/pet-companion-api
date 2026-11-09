package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.AddFoodMode;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code clientId} makes an offline retry safe. {@code mode} answers the "second main food" question;
 * {@code startedOn} lets the owner say the dog was already eating it.
 */
public record AddPetFoodRequest(UUID clientId,
                                @NotNull UUID productId,
                                @NotNull FoodKind kind,
                                Integer dailyGrams,
                                Boolean forTraining,
                                AddFoodMode mode,
                                LocalDate startedOn,
                                Boolean confirmAllergy,
                                @NotNull LocalDate date) {
}
