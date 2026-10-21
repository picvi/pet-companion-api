package com.picvi.pet_companion_service.dayevent.domain;

import java.time.LocalDate;
import java.util.UUID;

public record EventDefinition(UUID id,
                              DayEventType type,
                              String title,
                              LocalDate firstDate,
                              int startMinute,
                              int durationMinutes,
                              Recurrence recurrence,
                              boolean planAround) {
}
