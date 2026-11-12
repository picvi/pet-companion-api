package com.picvi.pet_companion_service.auth.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picvi.pet_companion_service.auth.dto.ForgotPasswordRequest;
import com.picvi.pet_companion_service.auth.dto.LoginResponse;
import com.picvi.pet_companion_service.auth.dto.ResetPasswordRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AuthContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void forgotPasswordRequestFixtureRoundTrips() throws IOException {
        assertRoundTrips("/contracts/forgot-password-request.json", ForgotPasswordRequest.class);
    }

    @Test
    void resetPasswordRequestFixtureRoundTrips() throws IOException {
        assertRoundTrips("/contracts/reset-password-request.json", ResetPasswordRequest.class);
    }

    @Test
    void resetPasswordResponseFixtureRoundTrips() throws IOException {
        assertRoundTrips("/contracts/reset-password-response.json", LoginResponse.class);
    }

    private <T> void assertRoundTrips(String fixture, Class<T> type) throws IOException {
        JsonNode expected = this.mapper.readTree(this.getClass().getResourceAsStream(fixture));
        T parsed = this.mapper.readValue(this.getClass().getResourceAsStream(fixture), type);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }
}
