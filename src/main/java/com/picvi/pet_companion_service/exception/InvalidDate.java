package com.picvi.pet_companion_service.exception;

public class InvalidDate extends ApiException {
    public InvalidDate() {
        super("Date is out of the allowed range", "INVALID_DATE");
    }
}
