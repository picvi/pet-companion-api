package com.picvi.pet_companion_service.exception;

public class PasswordIncorrect extends ApiException {
    public PasswordIncorrect() {
      super("Password is incorrect", "INCORRECT_PASSWORD");
    }
}
