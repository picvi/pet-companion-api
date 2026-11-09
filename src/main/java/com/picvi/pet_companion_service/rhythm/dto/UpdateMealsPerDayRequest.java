package com.picvi.pet_companion_service.rhythm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateMealsPerDayRequest(@NotNull @Min(1) @Max(12) Integer mealsPerDay, @NotNull LocalDate date) {
}
