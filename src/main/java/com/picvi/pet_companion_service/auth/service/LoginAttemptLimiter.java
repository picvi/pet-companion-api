package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.exception.TooManyLoginAttempts;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-account brake on password guessing, independent of the caller's IP. Unknown usernames are counted the same way,
 * so the lock does not reveal which accounts exist. State is in memory, like the IP rate limiter.
 * <p>
 * Two instances exist (see {@code AttemptLimiterConfig}): one for login keyed by username, and one for
 * re-authentication ({@code POST /me/password}, {@code POST /me/deletion}) keyed by user id.
 */
public class LoginAttemptLimiter {
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int PURGE_THRESHOLD = 10_000;

    private final Clock clock;
    private final int maxFailures;
    private final Duration lock;
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptLimiter(Clock clock, int maxFailures, Duration lock) {
        this.clock = clock;
        this.maxFailures = maxFailures;
        this.lock = lock;
    }

    public void checkAllowed(String username) {
        Attempts current = this.attempts.get(key(username));
        Instant now = this.clock.instant();
        if (current != null && current.isLocked(now)) {
            throw new TooManyLoginAttempts(Duration.between(now, current.lockedUntil()).toSeconds() + 1);
        }
    }

    public void recordFailure(String username) {
        Instant now = this.clock.instant();
        if (this.attempts.size() > PURGE_THRESHOLD) {
            this.attempts.values().removeIf(entry -> entry.isStale(now));
        }
        this.attempts.compute(key(username), (name, current) -> {
            Attempts base = current == null || current.isStale(now) ? Attempts.EMPTY : current;
            return base.afterFailure(now, this.maxFailures, this.lock);
        });
    }

    public void recordSuccess(String username) {
        this.attempts.remove(key(username));
    }

    private static String key(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private record Attempts(int failures, Instant windowStart, Instant lockedUntil) {
        static final Attempts EMPTY = new Attempts(0, null, null);

        boolean isLocked(Instant now) {
            return this.lockedUntil != null && now.isBefore(this.lockedUntil);
        }

        boolean isStale(Instant now) {
            if (this.lockedUntil != null) {
                return !now.isBefore(this.lockedUntil);
            }
            return this.windowStart != null && !now.isBefore(this.windowStart.plus(WINDOW));
        }

        Attempts afterFailure(Instant now, int maxFailures, Duration lockDuration) {
            Instant start = this.windowStart == null ? now : this.windowStart;
            int count = this.failures + 1;
            Instant lock = count >= maxFailures ? now.plus(lockDuration) : null;
            return new Attempts(count, start, lock);
        }
    }
}
