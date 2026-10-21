package com.picvi.pet_companion_service.exception.rhythm;

import com.picvi.pet_companion_service.exception.ApiException;

public class MealsOutOfRange extends ApiException {
    public MealsOutOfRange() {
        super("Number of meals is outside the range for this age", "MEALS_OUT_OF_RANGE");
    }
}
