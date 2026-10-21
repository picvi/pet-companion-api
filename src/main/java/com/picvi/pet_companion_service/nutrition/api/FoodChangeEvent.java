package com.picvi.pet_companion_service.nutrition.api;

import java.time.LocalDate;
import java.util.UUID;

/** Something that happened in a food change, for the timeline. The id is stable: the same event always has the same id. */
public record FoodChangeEvent(UUID id, Kind kind, LocalDate date, UUID transitionId) {

    public enum Kind {
        STARTED, SLOWED, PAUSED, FINISHED, CANCELLED, ALL_AT_ONCE
    }
}
