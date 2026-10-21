package com.picvi.pet_companion_service.exception.medical;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidVetVisit extends ApiException {
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";
    public static final String REQUIRED = "REQUIRED";
    public static final String INVALID = "INVALID";
    public static final String TOO_LONG = "TOO_LONG";

    public InvalidVetVisit(String field, String reason) {
        super(field + ": " + reason, "INVALID_VET_VISIT");
    }
}
