package com.picvi.pet_companion_service.exception;

public class InvalidVerificationCode extends ApiException {
    public InvalidVerificationCode() {
        super("Verification code is invalid or expired", "INVALID_VERIFICATION_CODE");
    }
}
