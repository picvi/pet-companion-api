package com.picvi.pet_companion_service.common;

import com.picvi.pet_companion_service.exception.InvalidDate;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientDateValidatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);

    private final ClientDateValidator validator = new ClientDateValidator(
            Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC));

    @Test
    void yesterdayTodayAndTomorrowPass() {
        assertThatCode(() -> validator.validate(TODAY.minusDays(1))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(TODAY.plusDays(1))).doesNotThrowAnyException();
    }

    @Test
    void twoDaysBeforeIsRejected() {
        assertThatThrownBy(() -> validator.validate(TODAY.minusDays(2))).isInstanceOf(InvalidDate.class);
    }

    @Test
    void twoDaysAfterIsRejected() {
        assertThatThrownBy(() -> validator.validate(TODAY.plusDays(2))).isInstanceOf(InvalidDate.class);
    }

    @Test
    void nullIsRejected() {
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidDate.class);
    }
}
