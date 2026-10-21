package com.picvi.pet_companion_service.exception.condition;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidCondition extends ApiException {
    public InvalidCondition(String reason) {
        super(reason, "INVALID_CONDITION");
    }
}
