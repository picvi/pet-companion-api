package com.picvi.pet_companion_service.appversion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppVersionControllerTest {
    private final AppVersionProperties properties = new AppVersionProperties(
            true,
            new AppVersionProperties.Store("1.0.0", "1.2.0", "https://apps.apple.com/app/id0000000000"),
            new AppVersionProperties.Store("1.0.0", "1.2.0", "https://play.google.com/store/apps/details?id=pet.curl"));
    private final AppVersionController controller = new AppVersionController(new AppVersionPolicy(this.properties));
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void answersPerPlatformCaseInsensitively() {
        ResponseEntity<VersionPolicyResponse> ios = this.controller.get("iOS");
        ResponseEntity<VersionPolicyResponse> android = this.controller.get("android");

        assertThat(ios.getBody().storeUrl()).contains("apple.com");
        assertThat(android.getBody().storeUrl()).contains("play.google.com");
    }

    @Test
    void unknownPlatformIsABadRequest() {
        assertThat(this.controller.get("windows").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void responseMatchesTheContractFixture() throws IOException {
        JsonNode expected = this.mapper.readTree(getClass().getResourceAsStream("/contracts/version-policy-response.json"));

        JsonNode actual = this.mapper.valueToTree(this.controller.get("ios").getBody());

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void invalidConfigurationStopsStartup() {
        AppVersionProperties broken = new AppVersionProperties(
                true,
                new AppVersionProperties.Store("one", "1.0.0", ""),
                this.properties.android());

        assertThatThrownBy(() -> new AppVersionPolicy(broken)).isInstanceOf(IllegalStateException.class);
    }
}
