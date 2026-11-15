package com.picvi.pet_companion_service.appversion;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class AppVersionFilterTest {
    private static final AppVersionProperties.Store IOS = new AppVersionProperties.Store("1.2.0", "1.3.0", "https://ios");
    private static final AppVersionProperties.Store ANDROID = new AppVersionProperties.Store("1.0.0", "1.3.0", "https://android");

    private final AppVersionFilter enforcing = filter(true);

    @Test
    void currentClientPasses() throws Exception {
        assertThat(get("/pets", "1.2.0", "ios", this.enforcing).getStatus()).isEqualTo(200);
        assertThat(get("/pets", "1.2.0+7", "ios", this.enforcing).getStatus()).isEqualTo(200);
        assertThat(get("/pets", "2.0.0", "ios", this.enforcing).getStatus()).isEqualTo(200);
    }

    @Test
    void oldClientGetsUpdateRequired() throws Exception {
        MockHttpServletResponse response = get("/pets", "1.1.9", "ios", this.enforcing);

        assertThat(response.getStatus()).isEqualTo(426);
        assertThat(response.getContentAsString()).contains("\"code\":\"APP_UPDATE_REQUIRED\"");
    }

    @Test
    void minimumIsPerPlatform() throws Exception {
        assertThat(get("/pets", "1.0.0", "android", this.enforcing).getStatus()).isEqualTo(200);
        assertThat(get("/pets", "1.0.0", "ios", this.enforcing).getStatus()).isEqualTo(426);
    }

    @Test
    void missingOrMalformedHeadersCountAsTooOld() throws Exception {
        assertThat(get("/pets", null, null, this.enforcing).getStatus()).isEqualTo(426);
        assertThat(get("/pets", "1.2.0", null, this.enforcing).getStatus()).isEqualTo(426);
        assertThat(get("/pets", "latest", "ios", this.enforcing).getStatus()).isEqualTo(426);
        assertThat(get("/pets", "1.2.0", "windows", this.enforcing).getStatus()).isEqualTo(426);
    }

    @Test
    void healthCheckAndPolicyStayOpen() throws Exception {
        assertThat(get("/actuator/health", null, null, this.enforcing).getStatus()).isEqualTo(200);
        assertThat(get("/app/version-policy", null, null, this.enforcing).getStatus()).isEqualTo(200);
    }

    @Test
    void doesNothingWhenNotEnforced() throws Exception {
        assertThat(get("/pets", null, null, filter(false)).getStatus()).isEqualTo(200);
    }

    private static AppVersionFilter filter(boolean enabled) {
        return new AppVersionFilter(new AppVersionPolicy(new AppVersionProperties(enabled, IOS, ANDROID)));
    }

    private static MockHttpServletResponse get(String uri, String version, String platform, AppVersionFilter filter)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setRequestURI(uri);
        if (version != null) {
            request.addHeader(AppVersionFilter.VERSION_HEADER, version);
        }
        if (platform != null) {
            request.addHeader(AppVersionFilter.PLATFORM_HEADER, platform);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
