package com.picvi.pet_companion_service.exception;

public class InvalidCredentials extends ApiException {
    public InvalidCredentials() {
        super("Username or password is incorrect", "INVALID_CREDENTIALS");
    }
}
