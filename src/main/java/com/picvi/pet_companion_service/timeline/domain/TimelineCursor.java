package com.picvi.pet_companion_service.timeline.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;

import java.time.LocalDate;
import java.util.UUID;

public record TimelineCursor(LocalDate localDate, int minuteOfDay, TimelineKind kind, UUID id) {
    private static final UUID LARGEST_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    public static TimelineCursor parse(String raw) {
        try {
            String[] parts = raw.split(",", -1);
            if (parts.length != 4) {
                throw new InvalidCursor();
            }
            return new TimelineCursor(LocalDate.parse(parts[0]), Integer.parseInt(parts[1]), TimelineKind.valueOf(parts[2]),
                    UUID.fromString(parts[3]));
        } catch (RuntimeException e) {
            throw new InvalidCursor();
        }
    }

    public static TimelineCursor start(LocalDate today) {
        return new TimelineCursor(today.plusDays(2), 0, TimelineKind.LOG, LARGEST_ID);
    }

    public String format() {
        return this.localDate + "," + this.minuteOfDay + "," + this.kind + "," + this.id;
    }
}
