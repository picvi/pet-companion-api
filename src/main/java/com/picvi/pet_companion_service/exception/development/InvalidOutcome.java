package com.picvi.pet_companion_service.exception.development;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidOutcome extends ApiException {
    public InvalidOutcome() {
        super("Outcome does not fit this kind of activity", "INVALID_OUTCOME");
    }
}
