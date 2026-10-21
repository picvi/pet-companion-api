package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class FoodChangeNotFound extends ApiException {
    public FoodChangeNotFound() {
        super("This food change was not found", "FOOD_CHANGE_NOT_FOUND");
    }
}
