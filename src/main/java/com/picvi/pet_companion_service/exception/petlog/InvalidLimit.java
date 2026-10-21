package com.picvi.pet_companion_service.exception.petlog;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidLimit extends ApiException {
    public InvalidLimit() {
        super("The limit must be at least 1", "INVALID_LIMIT");
    }
}
