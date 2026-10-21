package com.picvi.pet_companion_service.account.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picvi.pet_companion_service.account.dto.ChangePasswordRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AccountContractTest {
    private static final String FIXTURE = "/contracts/change-password-request.json";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void changePasswordRequestFixtureRoundTrips() throws IOException {
        JsonNode expected = this.mapper.readTree(this.getClass().getResourceAsStream(FIXTURE));
        ChangePasswordRequest parsed = this.mapper.readValue(this.getClass().getResourceAsStream(FIXTURE),
                ChangePasswordRequest.class);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }
}
