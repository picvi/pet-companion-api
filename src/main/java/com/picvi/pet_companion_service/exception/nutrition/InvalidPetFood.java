package com.picvi.pet_companion_service.exception.nutrition;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidPetFood extends ApiException {
    public InvalidPetFood(String reason) {
        super(reason, "INVALID_PET_FOOD");
    }
}
