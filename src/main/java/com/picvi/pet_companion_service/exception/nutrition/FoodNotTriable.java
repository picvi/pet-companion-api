package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class FoodNotTriable extends ApiException {
    public FoodNotTriable() {
        super("This food is not safe to try", "FOOD_NOT_TRIABLE");
    }
}
