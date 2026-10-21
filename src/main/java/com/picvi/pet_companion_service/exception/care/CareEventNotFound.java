package com.picvi.pet_companion_service.exception.care;

import com.picvi.pet_companion_service.exception.ApiException;

public class CareEventNotFound extends ApiException {
    public CareEventNotFound() {
        super("Care event not found", "CARE_EVENT_NOT_FOUND");
    }
}
