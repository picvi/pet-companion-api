package com.picvi.pet_companion_service.exception;

public class UserAlreadyExistsException extends  ApiException {
    public UserAlreadyExistsException(String message, String code) {
        super(message, code);
    }
}
