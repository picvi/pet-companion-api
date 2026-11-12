package com.picvi.pet_companion_service.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {
    private final RateLimitFilter filter = new RateLimitFilter(true, false, Clock.systemUTC());

    @Test
    void forgotPasswordIsLimitedToFiveRequestsAnHourPerAddress() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(post("/auth/forgot-password", "1.1.1.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse sixth = post("/auth/forgot-password", "1.1.1.1");

        assertThat(sixth.getStatus()).isEqualTo(429);
        assertThat(sixth.getHeader("Retry-After")).isNotNull();
        assertThat(post("/auth/forgot-password", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void resetPasswordIsLimitedToTenRequestsPerQuarterHourPerAddress() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(post("/auth/reset-password", "1.1.1.1").getStatus()).isEqualTo(200);
        }

        assertThat(post("/auth/reset-password", "1.1.1.1").getStatus()).isEqualTo(429);
    }

    @Test
    void refreshIsLimitedToSixtyRequestsPerQuarterHourPerAddress() throws Exception {
        for (int i = 0; i < 60; i++) {
            assertThat(post("/auth/refresh", "1.1.1.1").getStatus()).isEqualTo(200);
        }

        assertThat(post("/auth/refresh", "1.1.1.1").getStatus()).isEqualTo(429);
    }

    private MockHttpServletResponse post(String uri, String address) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        this.filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
