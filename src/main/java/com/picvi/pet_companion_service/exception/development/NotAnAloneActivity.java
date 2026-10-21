package com.picvi.pet_companion_service.exception.development;

import com.picvi.pet_companion_service.exception.ApiException;

public class NotAnAloneActivity extends ApiException {
    public NotAnAloneActivity() {
        super("Sessions can only be logged for alone-time activities", "NOT_AN_ALONE_ACTIVITY");
    }
}
