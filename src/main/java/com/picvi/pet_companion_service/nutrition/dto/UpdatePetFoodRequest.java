package com.picvi.pet_companion_service.nutrition.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdatePetFoodRequest(Integer dailyGrams, Boolean forTraining, @NotNull LocalDate date) {
}
