package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdatePetFoodRequest(Integer dailyGrams, Boolean forTraining, FoodOrigin origin, @NotNull LocalDate date) {
}
