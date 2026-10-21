package com.picvi.pet_companion_service.pet.domain;

import com.picvi.pet_companion_service.exception.pet.InvalidMicrochip;

import java.util.regex.Pattern;

public final class MicrochipNumber {
    private static final Pattern DIGITS = Pattern.compile("\\d{15}");

    private MicrochipNumber() {
    }

    public static String normalise(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String compact = raw.replace(" ", "");
        if (!DIGITS.matcher(compact).matches()) {
            throw new InvalidMicrochip();
        }
        return compact;
    }
}
