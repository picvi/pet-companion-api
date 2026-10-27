package com.picvi.pet_companion_service.auth.service;

import java.util.Locale;

/** Email addresses are stored and looked up in one canonical form, so Foo@x.com and foo@x.com are the same account. */
public final class EmailAddress {
    private EmailAddress() {
    }

    public static String normalise(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
