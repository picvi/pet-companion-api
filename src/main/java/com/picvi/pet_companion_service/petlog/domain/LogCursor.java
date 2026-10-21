package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;

import java.time.LocalDate;
import java.util.UUID;

public record LogCursor(LocalDate localDate, int minuteOfDay, UUID id) {
    private static final UUID LARGEST_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    public static LogCursor parse(String raw) {
        try {
            String[] parts = raw.split(",", -1);
            if (parts.length != 3) {
                throw new InvalidCursor();
            }
            return new LogCursor(LocalDate.parse(parts[0]), Integer.parseInt(parts[1]), UUID.fromString(parts[2]));
        } catch (RuntimeException e) {
            throw new InvalidCursor();
        }
    }

    public static LogCursor start(LocalDate today) {
        return new LogCursor(today.plusDays(2), 0, LARGEST_ID);
    }

    public String format() {
        return this.localDate + "," + this.minuteOfDay + "," + this.id;
    }
}
