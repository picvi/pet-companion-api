package com.picvi.pet_companion_service.medical.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.picvi.pet_companion_service.medical.dto.VetVisitListResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class VetVisitsContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void visitListFixtureRoundTrips() throws IOException {
        this.assertRoundTrip("vet-visits-list.json", VetVisitListResponse.class);
    }

    private <T> void assertRoundTrip(String name, Class<T> type) throws IOException {
        JsonNode expected = this.mapper.readTree(this.fixture(name));
        T parsed = this.mapper.readValue(this.fixture(name), type);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }

    private InputStream fixture(String name) {
        return this.getClass().getResourceAsStream("/contracts/" + name);
    }
}
