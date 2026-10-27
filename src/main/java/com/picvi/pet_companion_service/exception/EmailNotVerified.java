package com.picvi.pet_companion_service.exception;

import lombok.Getter;

@Getter
public class EmailNotVerified extends ApiException {
    private final String email;

    /** Login checks the password first, so only the account owner ever sees this email. */
    public EmailNotVerified(String email) {
        super("Email is not verified", "EMAIL_NOT_VERIFIED");
        this.email = email;
    }
}
