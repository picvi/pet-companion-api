package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.exception.PasswordTooCommon;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PasswordPolicy {
    private static final String COMMON_PASSWORDS = "security/common-passwords.txt";
    private static final int MIN_PERSONAL_PART = 4;

    private final Set<String> common = loadCommonPasswords();

    public void check(String password, String username, String email) {
        String lower = password.toLowerCase(Locale.ROOT);
        if (this.common.contains(lower)
                || isRepeatedCharacter(lower)
                || isSequence(lower)
                || containsPersonalPart(lower, username)
                || containsPersonalPart(lower, localPart(email))) {
            throw new PasswordTooCommon();
        }
    }

    private static boolean isRepeatedCharacter(String value) {
        return value.chars().distinct().count() == 1;
    }

    private static boolean isSequence(String value) {
        if (value.length() < 2) {
            return false;
        }
        int step = value.charAt(1) - value.charAt(0);
        if (Math.abs(step) != 1) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            if (value.charAt(i) - value.charAt(i - 1) != step) {
                return false;
            }
        }
        return true;
    }

    private static boolean containsPersonalPart(String password, String part) {
        if (part == null) {
            return false;
        }
        String normalised = part.trim().toLowerCase(Locale.ROOT);
        return normalised.length() >= MIN_PERSONAL_PART && password.contains(normalised);
    }

    private static String localPart(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static Set<String> loadCommonPasswords() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(COMMON_PASSWORDS).getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(line -> line.trim().toLowerCase(Locale.ROOT))
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
