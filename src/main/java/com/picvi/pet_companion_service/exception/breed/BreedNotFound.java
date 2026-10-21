package com.picvi.pet_companion_service.exception.breed;

import com.picvi.pet_companion_service.exception.ApiException;

public class BreedNotFound extends ApiException {
    public BreedNotFound() {
        super("Breed not found", "BREED_NOT_FOUND");
    }
}
