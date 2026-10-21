package com.picvi.pet_companion_service.auth.filter;

import com.picvi.pet_companion_service.auth.service.JwtService;
import com.picvi.pet_companion_service.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String SECRET = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = jwtService(Duration.ofMinutes(15));
        filter = new JwtAuthenticationFilter(jwtService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesRequestWithValidToken() throws Exception {
        String token = jwtService.generateToken(user());

        filter.doFilter(requestWithBearer(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(USER_ID);
    }

    @Test
    void anOrdinaryUserGetsOnlyTheUserRole() throws Exception {
        filter.doFilter(requestWithBearer(jwtService.generateToken(user())), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_USER");
    }

    @Test
    void anAdminTokenCarriesTheAdminRole() throws Exception {
        User admin = user();
        admin.setRole(com.picvi.pet_companion_service.user.entity.UserRole.ADMIN);

        filter.doFilter(requestWithBearer(jwtService.generateToken(admin)), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void ignoresExpiredToken() throws Exception {
        String token = jwtService(Duration.ofMinutes(-1)).generateToken(user());

        filter.doFilter(requestWithBearer(token), new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private JwtService jwtService(Duration accessTokenExpiration) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", SECRET);
        ReflectionTestUtils.setField(service, "accessTokenExpiration", accessTokenExpiration);
        return service;
    }

    private User user() {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private MockHttpServletRequest requestWithBearer(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }
}
