package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OccurredAtCalculatorTest {
    private final OccurredAtCalculator calculator = new OccurredAtCalculator(ZoneId.of("Europe/Warsaw"));

    @Test
    void usesTheOffsetFromThePhone() {
        LoggedAt at = this.calculator.at(LocalDate.of(2026, 10, 14), 570, 120);

        assertThat(at.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-10-14T09:30+02:00"));
        assertThat(at.utcOffsetMinutes()).isEqualTo(120);
    }

    @Test
    void fallsBackToWarsawWinterTime() {
        LoggedAt at = this.calculator.at(LocalDate.of(2026, 1, 10), 570, null);

        assertThat(at.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-01-10T09:30+01:00"));
        assertThat(at.utcOffsetMinutes()).isEqualTo(60);
    }

    @Test
    void fallsBackToWarsawSummerTime() {
        LoggedAt at = this.calculator.at(LocalDate.of(2026, 7, 10), 570, null);

        assertThat(at.occurredAt()).isEqualTo(OffsetDateTime.parse("2026-07-10T09:30+02:00"));
        assertThat(at.utcOffsetMinutes()).isEqualTo(120);
    }

    @Test
    void rejectsAnOffsetBeyondFourteenHours() {
        assertThatThrownBy(() -> this.calculator.at(LocalDate.of(2026, 10, 14), 570, 900))
                .isInstanceOfSatisfying(InvalidLog.class, ex -> {
                    assertThat(ex.getField()).isEqualTo(LogField.UTC_OFFSET_MINUTES);
                    assertThat(ex.getReason()).isEqualTo(InvalidLog.OUT_OF_RANGE);
                });
    }
}
