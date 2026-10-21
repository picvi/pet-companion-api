package com.picvi.pet_companion_service.health.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.picvi.pet_companion_service.health.dto.HealthSummaryResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class HealthSummaryContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void healthSummaryFixtureRoundTrips() throws IOException {
        JsonNode expected = this.mapper.readTree(this.fixture());
        HealthSummaryResponse parsed = this.mapper.readValue(this.fixture(), HealthSummaryResponse.class);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }

    private InputStream fixture() {
        return this.getClass().getResourceAsStream("/contracts/health-summary.json");
    }
}
