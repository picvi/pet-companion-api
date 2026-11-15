package com.picvi.pet_companion_service.appversion;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * Answers 426 to a client whose version is below the minimum for its platform. A request that does not say which
 * version it is counts as too old, so the check cannot be skipped by dropping the headers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class AppVersionFilter extends OncePerRequestFilter {
    public static final String VERSION_HEADER = "X-App-Version";
    public static final String PLATFORM_HEADER = "X-App-Platform";
    static final String UPDATE_REQUIRED_BODY =
            "{\"status\":426,\"code\":\"APP_UPDATE_REQUIRED\",\"message\":\"Please update the app to continue\"}";
    private static final Set<String> OPEN_PATHS = Set.of("/actuator/health", "/app/version-policy");

    private final AppVersionPolicy policy;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !this.policy.isEnforced() || OPEN_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<AppPlatform> platform = AppPlatform.parse(request.getHeader(PLATFORM_HEADER));
        Optional<AppVersion> version = AppVersion.parse(request.getHeader(VERSION_HEADER));
        if (platform.isPresent() && version.isPresent() && this.policy.isSupported(platform.get(), version.get())) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UPGRADE_REQUIRED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(UPDATE_REQUIRED_BODY.getBytes(StandardCharsets.UTF_8));
    }
}
