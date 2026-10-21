package com.picvi.pet_companion_service.exception.petlog;

import com.picvi.pet_companion_service.exception.ApiException;

public class LogNotFound extends ApiException {
    public LogNotFound() {
        super("Log not found", "LOG_NOT_FOUND");
    }
}
