package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class SafetyFoodNotFound extends ApiException {
    public SafetyFoodNotFound() {
        super("This food was not found", "SAFETY_FOOD_NOT_FOUND");
    }
}
