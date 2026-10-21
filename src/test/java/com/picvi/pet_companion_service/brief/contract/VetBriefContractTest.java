package com.picvi.pet_companion_service.brief.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse;
import com.picvi.pet_companion_service.medical.dto.CreateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class VetBriefContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void optionsFixtureRoundTrips() throws IOException {
        this.assertRoundTrip("vet-brief-options.json", BriefOptionsResponse.class);
    }

    @Test
    void briefFixtureRoundTrips() throws IOException {
        this.assertRoundTrip("vet-brief-response.json", VetBriefResponse.class);
    }

    @Test
    void visitFixtureRoundTrips() throws IOException {
        this.assertRoundTrip("vet-brief-visit.json", VetVisitDto.class);
    }

    @Test
    void createRequestFixtureReads() throws IOException {
        CreateVetVisitRequest request = this.mapper.readValue(this.fixture("vet-brief-visit-create-request.json"), CreateVetVisitRequest.class);

        assertThat(request.outcomes()).hasSize(2);
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
