package com.picvi.pet_companion_service.appversion;

import java.util.Locale;
import java.util.Optional;

public enum AppPlatform {
    IOS, ANDROID;

    public static Optional<AppPlatform> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
