package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class FoodNotActive extends ApiException {
    public FoodNotActive() {
        super("This food is not one of the pet's foods on that day", "FOOD_NOT_ACTIVE");
    }
}
