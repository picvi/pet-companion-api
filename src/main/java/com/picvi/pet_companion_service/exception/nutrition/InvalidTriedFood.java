package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidTriedFood extends ApiException {
    public InvalidTriedFood(String reason) {
        super(reason, "INVALID_TRIED_FOOD");
    }
}
