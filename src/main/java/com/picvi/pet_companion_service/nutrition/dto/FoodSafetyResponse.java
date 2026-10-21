package com.picvi.pet_companion_service.nutrition.dto;

import java.util.List;

public record FoodSafetyResponse(Review review, List<FoodSafetyItemDto> items) {
}
