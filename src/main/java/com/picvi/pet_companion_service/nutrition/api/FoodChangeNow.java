package com.picvi.pet_companion_service.nutrition.api;

import java.time.LocalDate;

/** The food change that matters for how the dog is doing on a day. */
public record FoodChangeNow(State state, LocalDate startedOn) {

    public enum State {
        RUNNING, PAUSED, ENDED
    }
}
