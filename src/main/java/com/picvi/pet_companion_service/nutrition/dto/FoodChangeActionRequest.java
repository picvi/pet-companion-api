package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.CancelKeep;
import com.picvi.pet_companion_service.nutrition.domain.TransitionAction;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** {@code keep} says which food stays after CANCEL (default: the old one). */
public record FoodChangeActionRequest(@NotNull TransitionAction action, CancelKeep keep, @NotNull LocalDate date) {
}
