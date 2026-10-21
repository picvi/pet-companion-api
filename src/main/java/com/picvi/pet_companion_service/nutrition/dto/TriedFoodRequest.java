package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.TriedReaction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TriedFoodRequest(@NotNull LocalDate date, @NotNull LocalDate triedOn, @NotNull TriedReaction reaction, String note) {
}
