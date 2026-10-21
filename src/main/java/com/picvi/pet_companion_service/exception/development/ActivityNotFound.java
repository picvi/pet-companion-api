package com.picvi.pet_companion_service.exception.development;

import com.picvi.pet_companion_service.exception.ApiException;

public class ActivityNotFound extends ApiException {
    public ActivityNotFound() {
        super("Activity not found", "ACTIVITY_NOT_FOUND");
    }
}
