package com.picvi.pet_companion_service.exception.condition;

import com.picvi.pet_companion_service.exception.ApiException;

public class ConditionNotFound extends ApiException {
    public ConditionNotFound() {
        super("This condition was not found", "CONDITION_NOT_FOUND");
    }
}
