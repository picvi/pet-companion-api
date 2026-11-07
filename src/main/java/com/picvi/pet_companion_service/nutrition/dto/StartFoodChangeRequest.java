package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Starts a planned change (STANDARD, SLOW) or an all-at-once switch (IMMEDIATE) to {@code productId}.
 * {@code fromFoodId} is needed only when two main foods are being given. {@code dailyGrams} defaults to the old food's.
 */
public record StartFoodChangeRequest(UUID clientId,
                                     @NotNull UUID productId,
                                     UUID fromFoodId,
                                     @NotNull TransitionPace pace,
                                     LocalDate startOn,
                                     Integer dailyGrams,
                                     Boolean confirmAllergy,
                                     @NotNull LocalDate date) {
}
