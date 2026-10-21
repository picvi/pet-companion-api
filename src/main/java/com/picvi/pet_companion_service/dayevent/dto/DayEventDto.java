package com.picvi.pet_companion_service.dayevent.dto;

import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.EventDefinition;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrence;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;

import java.time.LocalDate;
import java.util.UUID;

public record DayEventDto(UUID id,
                          DayEventType type,
                          String title,
                          LocalDate localDate,
                          int startMinute,
                          int durationMinutes,
                          Recurrence recurrence,
                          boolean planAround) {

    public static DayEventDto of(EventOccurrence occurrence, LocalDate date) {
        return new DayEventDto(occurrence.eventId(), occurrence.type(), occurrence.title(), date, occurrence.startMinute(),
                occurrence.durationMinutes(), occurrence.recurrence(), occurrence.planAround());
    }

    public static DayEventDto of(EventDefinition event) {
        return new DayEventDto(event.id(), event.type(), event.title(), event.firstDate(), event.startMinute(),
                event.durationMinutes(), event.recurrence(), event.planAround());
    }
}
