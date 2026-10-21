package com.picvi.pet_companion_service.nutrition.dto;

import java.util.List;

public record FoodProductsResponse(List<FoodProductDto> items, String nextCursor) {
}
