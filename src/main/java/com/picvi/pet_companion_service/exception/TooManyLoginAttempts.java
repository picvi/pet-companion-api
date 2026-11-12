package com.picvi.pet_companion_service.exception;

public class TooManyLoginAttempts extends ApiException {
    private final long retryAfterSeconds;

    public TooManyLoginAttempts(long retryAfterSeconds) {
        super("Too many failed login attempts, try again later", "TOO_MANY_REQUESTS");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return this.retryAfterSeconds;
    }
}
