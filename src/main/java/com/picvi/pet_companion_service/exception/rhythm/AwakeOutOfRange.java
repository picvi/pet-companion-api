package com.picvi.pet_companion_service.exception.rhythm;

import com.picvi.pet_companion_service.exception.ApiException;

public class AwakeOutOfRange extends ApiException {
    public AwakeOutOfRange() {
        super("Awake time is outside the range for this age", "AWAKE_OUT_OF_RANGE");
    }
}
