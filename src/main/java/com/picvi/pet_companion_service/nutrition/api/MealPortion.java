package com.picvi.pet_companion_service.nutrition.api;

import java.util.UUID;

/** Grams of one food in one meal; grams is null when the food has no daily amount ("parts"). */
public record MealPortion(UUID petFoodId, Integer grams) {
}
