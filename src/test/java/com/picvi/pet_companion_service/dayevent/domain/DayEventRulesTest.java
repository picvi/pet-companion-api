package com.picvi.pet_companion_service.dayevent.domain;

import com.picvi.pet_companion_service.exception.dayevent.InvalidDayEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DayEventRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    private final DayEventRules rules = new DayEventRules();

    @Test
    void acceptsAnEventTodayOrLater() {
        assertThatCode(() -> this.rules.validate(600, 60, "Dr Kim", TODAY, TODAY)).doesNotThrowAnyException();
        assertThatCode(() -> this.rules.validate(600, 60, null, TODAY.plusDays(30), TODAY)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnEventInThePast() {
        assertThatThrownBy(() -> this.rules.validate(600, 60, null, TODAY.minusDays(1), TODAY)).isInstanceOf(InvalidDayEvent.class);
    }

    @Test
    void rejectsADurationOutsideFifteenMinutesToTwelveHours() {
        assertThatThrownBy(() -> this.rules.validate(600, 10, null, TODAY, TODAY)).isInstanceOf(InvalidDayEvent.class);
        assertThatThrownBy(() -> this.rules.validate(0, 721, null, TODAY, TODAY)).isInstanceOf(InvalidDayEvent.class);
    }

    @Test
    void rejectsAnEventThatRunsPastMidnight() {
        assertThatThrownBy(() -> this.rules.validate(1400, 60, null, TODAY, TODAY)).isInstanceOf(InvalidDayEvent.class);
    }

    @Test
    void rejectsATooLongTitle() {
        assertThatThrownBy(() -> this.rules.validate(600, 60, "x".repeat(61), TODAY, TODAY)).isInstanceOf(InvalidDayEvent.class);
    }

    @Test
    void aMovedStartMustKeepTheEventInsideTheDay() {
        assertThatCode(() -> this.rules.validateStart(600, 60)).doesNotThrowAnyException();
        assertThatThrownBy(() -> this.rules.validateStart(1400, 60)).isInstanceOf(InvalidDayEvent.class);
        assertThatThrownBy(() -> this.rules.validateStart(-1, 60)).isInstanceOf(InvalidDayEvent.class);
    }
}
