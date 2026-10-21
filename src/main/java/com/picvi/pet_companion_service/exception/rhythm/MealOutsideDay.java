package com.picvi.pet_companion_service.exception.rhythm;

import com.picvi.pet_companion_service.exception.ApiException;

public class MealOutsideDay extends ApiException {
    public MealOutsideDay() {
        super("Meals must fall between waking up and the evening wind-down", "MEAL_OUTSIDE_DAY");
    }
}
