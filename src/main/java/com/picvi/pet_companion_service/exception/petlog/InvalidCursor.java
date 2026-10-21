package com.picvi.pet_companion_service.exception.petlog;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidCursor extends ApiException {
    public InvalidCursor() {
        super("The cursor is not valid", "INVALID_CURSOR");
    }
}
