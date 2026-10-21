package com.picvi.pet_companion_service.exception;

public class InvalidRefreshToken extends ApiException {
    public InvalidRefreshToken() {
        super("Refresh token is invalid or expired", "INVALID_REFRESH_TOKEN");
    }
}
