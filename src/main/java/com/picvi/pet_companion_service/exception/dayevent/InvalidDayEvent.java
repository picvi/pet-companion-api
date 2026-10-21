package com.picvi.pet_companion_service.exception.dayevent;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidDayEvent extends ApiException {
    public InvalidDayEvent() {
        super("Event time, length or date is not valid", "INVALID_DAY_EVENT");
    }
}
