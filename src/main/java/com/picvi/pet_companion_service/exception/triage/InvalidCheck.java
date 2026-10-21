package com.picvi.pet_companion_service.exception.triage;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidCheck extends ApiException {
    public InvalidCheck(String field, String reason) {
        super(field + ": " + reason, "INVALID_CHECK");
    }
}
