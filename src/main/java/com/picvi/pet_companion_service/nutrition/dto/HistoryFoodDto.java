package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.FoodKind;

import java.time.LocalDate;
import java.util.UUID;

/** A food the dog was given in the period. {@code from} and {@code to} are the first and last day given; {@code to} is null while it is still given. */
public record HistoryFoodDto(UUID petFoodId,
                             UUID productId,
                             FoodKind kind,
                             String brand,
                             String productName,
                             String flavour,
                             LocalDate from,
                             LocalDate to,
                             Integer dailyGrams) {
}
