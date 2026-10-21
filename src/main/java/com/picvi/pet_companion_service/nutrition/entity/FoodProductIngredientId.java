package com.picvi.pet_companion_service.nutrition.entity;

import java.io.Serializable;
import java.util.UUID;

public record FoodProductIngredientId(UUID productId, String sourceCode) implements Serializable {
}
