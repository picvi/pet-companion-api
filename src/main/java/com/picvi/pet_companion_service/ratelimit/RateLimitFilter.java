package com.picvi.pet_companion_service.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/**
 * Per-IP limits on the public auth endpoints, which are the ones that cost money (mail) or can be brute-forced, and on
 * the authenticated re-authentication endpoints (password change, account deletion), where they only blunt bursts: the
 * per-user lock in {@code reauthAttemptLimiter} is the real protection there.
 * With trust-forwarded-for (behind Caddy, app port not published) the client address is the last X-Forwarded-For entry,
 * the one Caddy itself appended; otherwise the header is ignored because anyone could forge it.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final boolean enabled;
    private final boolean trustForwardedFor;
    private final Map<String, SlidingWindowRateLimiter> limiters;

    public RateLimitFilter(@Value("${pawstep.rate-limit.enabled:true}") boolean enabled,
                           @Value("${pawstep.rate-limit.trust-forwarded-for:false}") boolean trustForwardedFor,
                           Clock clock) {
        this.enabled = enabled;
        this.trustForwardedFor = trustForwardedFor;
        this.limiters = Map.of(
                "/auth/register", new SlidingWindowRateLimiter(5, Duration.ofHours(1), clock),
                "/auth/resend-verification", new SlidingWindowRateLimiter(5, Duration.ofHours(1), clock),
                "/auth/verify-email", new SlidingWindowRateLimiter(10, Duration.ofMinutes(15), clock),
                "/auth/forgot-password", new SlidingWindowRateLimiter(5, Duration.ofHours(1), clock),
                "/auth/reset-password", new SlidingWindowRateLimiter(10, Duration.ofMinutes(15), clock),
                "/auth/refresh", new SlidingWindowRateLimiter(60, Duration.ofMinutes(15), clock),
                "/auth/login", new SlidingWindowRateLimiter(20, Duration.ofMinutes(15), clock),
                "/me/password", new SlidingWindowRateLimiter(10, Duration.ofMinutes(15), clock),
                "/me/deletion", new SlidingWindowRateLimiter(10, Duration.ofMinutes(15), clock));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !this.enabled || !"POST".equals(request.getMethod()) || !this.limiters.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SlidingWindowRateLimiter limiter = this.limiters.get(request.getRequestURI());
        String key = this.clientIp(request);
        if (limiter.tryAcquire(key)) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(limiter.retryAfterSeconds(key)));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(
                "{\"status\":429,\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many requests, try again later\"}"
                        .getBytes(StandardCharsets.UTF_8));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (this.trustForwardedFor && forwarded != null && !forwarded.isBlank()) {
            String[] parts = forwarded.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
