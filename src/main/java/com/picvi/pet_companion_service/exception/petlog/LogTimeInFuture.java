package com.picvi.pet_companion_service.exception.petlog;

import com.picvi.pet_companion_service.exception.ApiException;

public class LogTimeInFuture extends ApiException {
    public LogTimeInFuture() {
        super("A log cannot be dated after today", "LOG_TIME_IN_FUTURE");
    }
}
