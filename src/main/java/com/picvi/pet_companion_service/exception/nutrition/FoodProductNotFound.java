package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class FoodProductNotFound extends ApiException {
    public FoodProductNotFound() {
        super("This food product was not found", "FOOD_PRODUCT_NOT_FOUND");
    }
}
