package com.picvi.pet_companion_service.exception;

public class PasswordTooCommon extends ApiException {
    public PasswordTooCommon() {
        super("Password is too easy to guess", "PASSWORD_TOO_COMMON");
    }
}
