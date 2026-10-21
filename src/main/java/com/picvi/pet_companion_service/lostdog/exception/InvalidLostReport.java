package com.picvi.pet_companion_service.lostdog.exception;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidLostReport extends ApiException {
    public InvalidLostReport(String message) {
        super(message, "INVALID_LOST_REPORT");
    }
}
