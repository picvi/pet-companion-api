package com.picvi.pet_companion_service.dayevent.domain;

import java.time.LocalDate;
import java.util.UUID;

public record OccurrenceChange(UUID eventId, LocalDate date, boolean skipped, Integer startMinute) {
}
