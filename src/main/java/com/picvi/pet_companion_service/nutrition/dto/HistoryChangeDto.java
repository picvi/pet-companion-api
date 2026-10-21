package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.ChangeMethod;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;

import java.time.LocalDate;
import java.util.UUID;

/** A change of main food and how it was done. {@code to} is the last day of the plan, or null while it runs. */
public record HistoryChangeDto(UUID transitionId,
                               ChangeMethod method,
                               TransitionPace pace,
                               UUID fromFoodId,
                               UUID toFoodId,
                               LocalDate from,
                               LocalDate to) {
}
