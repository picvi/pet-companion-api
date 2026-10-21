package com.picvi.pet_companion_service.nutrition.api;

import com.picvi.pet_companion_service.nutrition.domain.FoodKind;

import java.util.UUID;

public record ActiveFood(UUID petFoodId, UUID productId, FoodKind kind) {
}
