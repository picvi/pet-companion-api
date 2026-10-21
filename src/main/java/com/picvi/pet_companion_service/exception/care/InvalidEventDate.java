package com.picvi.pet_companion_service.exception.care;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidEventDate extends ApiException {
    public InvalidEventDate() {
        super("Date must not be in the future or before the pet was born", "INVALID_EVENT_DATE");
    }
}
