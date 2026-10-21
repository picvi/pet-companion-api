package com.picvi.pet_companion_service.exception.dayevent;

import com.picvi.pet_companion_service.exception.ApiException;

public class DayEventNotFound extends ApiException {
    public DayEventNotFound() {
        super("Event not found", "EVENT_NOT_FOUND");
    }
}
