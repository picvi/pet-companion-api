package com.picvi.pet_companion_service.exception.pet;

import com.picvi.pet_companion_service.exception.ApiException;

public class PetNotFound extends ApiException {
    public PetNotFound() {
        super("Pet not found", "PET_NOT_FOUND");
    }
}
