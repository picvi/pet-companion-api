package com.picvi.pet_companion_service.exception.brief;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidBriefRequest extends ApiException {
    public static final String REQUIRED = "REQUIRED";
    public static final String INVALID = "INVALID";
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";

    public InvalidBriefRequest(String field, String reason) {
        super(field + ": " + reason, "INVALID_BRIEF_REQUEST");
    }
}
