package com.picvi.pet_companion_service.auth.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Two independent per-account brakes on password guessing. Login is keyed by username; re-authentication (change
 * password, delete account) is keyed by user id and shared by both endpoints, so a stolen access token cannot turn
 * either of them into a guessing oracle.
 */
@Configuration
public class AttemptLimiterConfig {
    public static final String LOGIN = "loginAttemptLimiter";
    public static final String REAUTH = "reauthAttemptLimiter";

    static final int LOGIN_MAX_FAILURES = 10;
    static final int REAUTH_MAX_FAILURES = 5;
    static final Duration LOCK = Duration.ofMinutes(15);

    @Bean(LOGIN)
    public LoginAttemptLimiter loginAttemptLimiter(Clock clock) {
        return new LoginAttemptLimiter(clock, LOGIN_MAX_FAILURES, LOCK);
    }

    @Bean(REAUTH)
    public LoginAttemptLimiter reauthAttemptLimiter(Clock clock) {
        return new LoginAttemptLimiter(clock, REAUTH_MAX_FAILURES, LOCK);
    }
}
