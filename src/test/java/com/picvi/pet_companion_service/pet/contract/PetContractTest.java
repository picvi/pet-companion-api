package com.picvi.pet_companion_service.pet.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.dto.UpdatePetRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class PetContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void updateRequestFixtureRoundTrips() throws IOException {
        assertRoundTrips("/contracts/pet-update-request.json", UpdatePetRequest.class);
    }

    @Test
    void petResponseFixtureRoundTrips() throws IOException {
        assertRoundTrips("/contracts/pet-response.json", PetResponse.class);
    }

    private void assertRoundTrips(String fixture, Class<?> type) throws IOException {
        JsonNode expected = this.mapper.readTree(this.open(fixture));
        Object parsed = this.mapper.readValue(this.open(fixture), type);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }

    private InputStream open(String fixture) {
        return this.getClass().getResourceAsStream(fixture);
    }
}
