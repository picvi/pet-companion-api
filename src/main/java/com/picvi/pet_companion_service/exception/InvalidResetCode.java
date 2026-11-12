package com.picvi.pet_companion_service.exception;

public class InvalidResetCode extends ApiException {
    public InvalidResetCode() {
        super("Reset code is invalid or expired", "INVALID_RESET_CODE");
    }
}
