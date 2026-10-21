package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidFoodChange extends ApiException {
    public InvalidFoodChange(String reason) {
        super(reason, "INVALID_FOOD_CHANGE");
    }
}
