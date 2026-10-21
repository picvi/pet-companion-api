package com.picvi.pet_companion_service.exception.rhythm;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidDayTimes extends ApiException {
    public InvalidDayTimes() {
        super("Wake-up and bed times must leave a day of 10 to 20 hours", "INVALID_DAY_TIMES");
    }
}
