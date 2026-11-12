package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.exception.PasswordTooCommon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @ParameterizedTest
    @ValueSource(strings = {"password123", "PASSWORD1234", "1234567890", "1111111111", "abcdefghij", "0987654321", "qwertyuiop",
            "Curlup-2026-x", "my-pawstep-pw"})
    void rejectsCommonRepeatedAndSequentialPasswords(String password) {
        assertThatThrownBy(() -> this.policy.check(password, "viki", "viki@example.com"))
                .isInstanceOf(PasswordTooCommon.class);
    }

    @Test
    void rejectsPasswordsContainingTheUsernameOrEmailName() {
        assertThatThrownBy(() -> this.policy.check("Wiktoria-2026-x", "wiktoria", "other@example.com"))
                .isInstanceOf(PasswordTooCommon.class);
        assertThatThrownBy(() -> this.policy.check("zz-marta.k-zz", "viki", "marta.k@example.com"))
                .isInstanceOf(PasswordTooCommon.class);
    }

    @Test
    void ignoresShortPersonalPartsAndAcceptsOrdinaryStrongPasswords() {
        assertThatCode(() -> this.policy.check("vk-blue-lamp-47", "vk", "vk@example.com")).doesNotThrowAnyException();
        assertThatCode(() -> this.policy.check("old-secret-1", "viki", "viki@example.com")).doesNotThrowAnyException();
    }
}
