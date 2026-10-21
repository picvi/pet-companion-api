package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class PetFoodNotFound extends ApiException {
    public PetFoodNotFound() {
        super("This food was not found", "PET_FOOD_NOT_FOUND");
    }
}
