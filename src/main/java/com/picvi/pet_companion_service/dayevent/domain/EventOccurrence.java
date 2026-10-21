package com.picvi.pet_companion_service.dayevent.domain;

import java.util.UUID;

public record EventOccurrence(UUID eventId,
                              DayEventType type,
                              String title,
                              int startMinute,
                              int durationMinutes,
                              Recurrence recurrence,
                              boolean planAround) {

    public int endMinute() {
        return this.startMinute + this.durationMinutes;
    }
}
