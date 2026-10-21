package com.picvi.pet_companion_service.exception.account;

import com.picvi.pet_companion_service.exception.ApiException;

public class PasswordUnchanged extends ApiException {
    public PasswordUnchanged() {
        super("New password must differ from the current one", "PASSWORD_UNCHANGED");
    }
}
