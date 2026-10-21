package com.picvi.pet_companion_service.exception.account;

import com.picvi.pet_companion_service.exception.ApiException;

public class CurrentPasswordIncorrect extends ApiException {
    public CurrentPasswordIncorrect() {
        super("Current password is incorrect", "CURRENT_PASSWORD_INCORRECT");
    }
}
