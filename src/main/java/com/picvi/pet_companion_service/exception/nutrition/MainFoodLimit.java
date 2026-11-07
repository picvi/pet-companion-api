package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class MainFoodLimit extends ApiException {
    public MainFoodLimit() {
        super("She can have at most two main foods", "MAIN_FOOD_LIMIT");
    }
}
