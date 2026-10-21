package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class FoodChangeAlreadyActive extends ApiException {
    public FoodChangeAlreadyActive() {
        super("A food change is already running. Finish or cancel it first", "FOOD_CHANGE_ALREADY_ACTIVE");
    }
}
