package com.picvi.pet_companion_service.exception.care;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidInterval extends ApiException {
    public InvalidInterval() {
        super("Interval must be between 7 and 365 days", "INVALID_INTERVAL");
    }
}
