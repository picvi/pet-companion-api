package com.picvi.pet_companion_service.exception.pet;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidMicrochip extends ApiException {
    public InvalidMicrochip() {
        super("Chip number must be 15 digits", "INVALID_MICROCHIP");
    }
}
