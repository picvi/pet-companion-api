package com.picvi.pet_companion_service.exception.development;

import com.picvi.pet_companion_service.exception.ApiException;

public class ActivityAlreadyCompleted extends ApiException {
    public ActivityAlreadyCompleted() {
        super("Activity is already completed", "ACTIVITY_ALREADY_COMPLETED");
    }
}
