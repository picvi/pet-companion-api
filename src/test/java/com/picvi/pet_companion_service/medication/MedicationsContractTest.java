package com.picvi.pet_companion_service.medication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.picvi.pet_companion_service.medication.dto.CoursesResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class MedicationsContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void coursesListFixtureRoundTrips() throws IOException {
        JsonNode expected = this.mapper.readTree(this.getClass().getResourceAsStream("/contracts/medications-list.json"));
        CoursesResponse parsed = this.mapper.readValue(this.getClass().getResourceAsStream("/contracts/medications-list.json"), CoursesResponse.class);

        JsonNode actual = this.mapper.readTree(this.mapper.writeValueAsString(parsed));

        assertThat(actual).isEqualTo(expected);
    }
}
