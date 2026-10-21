package com.picvi.pet_companion_service.exception.pet;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidBirthDate extends ApiException {
    public InvalidBirthDate() {
        super("Birth date must be a date that is not in the future", "INVALID_BIRTH_DATE");
    }
}
