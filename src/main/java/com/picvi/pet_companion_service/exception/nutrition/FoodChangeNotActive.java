package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class FoodChangeNotActive extends ApiException {
    public FoodChangeNotActive() {
        super("This food change is not running", "FOOD_CHANGE_NOT_ACTIVE");
    }
}
