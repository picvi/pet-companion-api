package com.picvi.pet_companion_service.nutrition.dto;

import java.util.List;

/** Owner-added products with the same normalised name: one thing owners typed in several times. */
public record PendingFoodGroupDto(String normalisedName, int ownerCount, List<FoodProductDto> products) {
}
