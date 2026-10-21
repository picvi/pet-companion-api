package com.picvi.pet_companion_service.petlog.contract;

import com.picvi.pet_companion_service.petlog.dto.CreatePetLogRequest;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyClientRequestTest extends PostgresTest {
    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void aRequestStillCarryingSourceIsAccepted() {
        String body = """
                {"clientId":"6f1b2c3d-0000-4000-8000-000000000001","type":"WEIGHT","day":"2026-10-10",
                 "minuteOfDay":600,"weightGrams":8200,"source":"WEIGH_IN"}""";

        CreatePetLogRequest request = jsonMapper.readValue(body, CreatePetLogRequest.class);

        assertThat(request.weightGrams()).isEqualTo(8200);
    }
}
