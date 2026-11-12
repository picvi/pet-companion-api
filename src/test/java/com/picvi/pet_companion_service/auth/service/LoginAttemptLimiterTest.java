package com.picvi.pet_companion_service.auth.service;

import com.picvi.pet_companion_service.exception.TooManyLoginAttempts;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(this.clock);

    @Test
    void locksTheAccountAfterTenFailuresEvenForUnknownNames() {
        failTimes("Ghost", LoginAttemptLimiter.MAX_FAILURES);

        assertThatThrownBy(() -> this.limiter.checkAllowed("ghost")).isInstanceOf(TooManyLoginAttempts.class);
    }

    @Test
    void otherAccountsAreNotAffected() {
        failTimes("viki", LoginAttemptLimiter.MAX_FAILURES);

        assertThatCode(() -> this.limiter.checkAllowed("marta")).doesNotThrowAnyException();
    }

    @Test
    void lockEndsAfterTheLockPeriod() {
        failTimes("viki", LoginAttemptLimiter.MAX_FAILURES);
        this.clock.advance(LoginAttemptLimiter.LOCK.plusSeconds(1));

        assertThatCode(() -> this.limiter.checkAllowed("viki")).doesNotThrowAnyException();
    }

    @Test
    void successClearsTheFailureCount() {
        failTimes("viki", LoginAttemptLimiter.MAX_FAILURES - 1);
        this.limiter.recordSuccess("viki");
        failTimes("viki", LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> this.limiter.checkAllowed("viki")).doesNotThrowAnyException();
    }

    @Test
    void failuresOutsideTheWindowDoNotAccumulate() {
        failTimes("viki", LoginAttemptLimiter.MAX_FAILURES - 1);
        this.clock.advance(LoginAttemptLimiter.WINDOW.plusSeconds(1));
        failTimes("viki", LoginAttemptLimiter.MAX_FAILURES - 1);

        assertThatCode(() -> this.limiter.checkAllowed("viki")).doesNotThrowAnyException();
    }

    private void failTimes(String username, int times) {
        for (int i = 0; i < times; i++) {
            this.limiter.recordFailure(username);
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-10T10:00:00Z");

        void advance(Duration duration) {
            this.now = this.now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.now;
        }
    }
}
