package com.picvi.pet_companion_service.exception.triage;

import com.picvi.pet_companion_service.exception.ApiException;

public class CheckConflict extends ApiException {
    public CheckConflict(String message) {
        super(message, "CHECK_CONFLICT");
    }
}
