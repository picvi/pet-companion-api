package com.picvi.pet_companion_service.exception;

public abstract class ApiException extends RuntimeException {
    private final String code;

    protected ApiException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
