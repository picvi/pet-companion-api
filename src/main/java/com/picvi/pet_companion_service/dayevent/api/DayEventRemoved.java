package com.picvi.pet_companion_service.dayevent.api;

import com.picvi.pet_companion_service.dayevent.domain.DayEventType;

import java.util.UUID;

/** Published before an owner deletes an event in her day, so modules that book through events can follow. */
public record DayEventRemoved(UUID petId, UUID eventId, DayEventType type) {
}
