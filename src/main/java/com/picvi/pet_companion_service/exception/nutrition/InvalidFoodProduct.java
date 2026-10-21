package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidFoodProduct extends ApiException {
    public InvalidFoodProduct(String reason) {
        super(reason, "INVALID_FOOD_PRODUCT");
    }
}
