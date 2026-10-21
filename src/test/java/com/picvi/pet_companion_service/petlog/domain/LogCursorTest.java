package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogCursorTest {

    @Test
    void formatAndParseRoundTrip() {
        LogCursor cursor = new LogCursor(LocalDate.of(2026, 10, 14), 570, UUID.randomUUID());

        assertThat(LogCursor.parse(cursor.format())).isEqualTo(cursor);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> LogCursor.parse("garbage")).isInstanceOf(InvalidCursor.class);
        assertThatThrownBy(() -> LogCursor.parse("2026-10-14,abc," + UUID.randomUUID())).isInstanceOf(InvalidCursor.class);
        assertThatThrownBy(() -> LogCursor.parse("2026-10-14,5,not-a-uuid")).isInstanceOf(InvalidCursor.class);
    }

    @Test
    void startIsAfterEverythingThatCanExist() {
        LogCursor start = LogCursor.start(LocalDate.of(2026, 10, 14));

        assertThat(start.localDate()).isEqualTo(LocalDate.of(2026, 10, 16));
    }
}
