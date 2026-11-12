package com.picvi.pet_companion_service.account.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picvi.pet_companion_service.account.dto.ChangePasswordRequest;
import com.picvi.pet_companion_service.account.dto.DeleteAccountRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AccountContractTest {
    private static final String FIXTURE = "/contracts/change-password-request.json";

    private static final String DELETE_FIXTURE = "/contracts/delete-account-request.json";

    private final ObjectMapper mapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void changePasswordRequestFixtureRoundTrips() throws IOException {
        JsonNode expected = this.mapper.readTree(this.getClass().getResourceAsStream(FIXTURE));
        ChangePasswordRequest parsed = this.mapper.readValue(this.getClass().getResourceAsStream(FIXTURE),
                ChangePasswordRequest.class);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void deleteAccountRequestFixtureRoundTrips() throws IOException {
        JsonNode expected = this.mapper.readTree(this.getClass().getResourceAsStream(DELETE_FIXTURE));
        DeleteAccountRequest parsed = this.mapper.readValue(this.getClass().getResourceAsStream(DELETE_FIXTURE),
                DeleteAccountRequest.class);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
        assertThat(parsed.password()).isEqualTo("old-secret-1");
        assertThat(this.validator.validate(parsed)).isEmpty();
    }

    @Test
    void blankDeletionPasswordFailsValidation() {
        assertThat(this.validator.validate(new DeleteAccountRequest(" "))).isNotEmpty();
        assertThat(this.validator.validate(new DeleteAccountRequest(null))).isNotEmpty();
    }

    @Test
    void deleteAccountRequestNeverPrintsThePassword() {
        assertThat(new DeleteAccountRequest("old-secret-1").toString()).doesNotContain("old-secret-1");
    }
}
