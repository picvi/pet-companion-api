package com.picvi.pet_companion_service.exception.petlog;

import com.picvi.pet_companion_service.exception.ApiException;
import com.picvi.pet_companion_service.petlog.domain.LogField;

public class InvalidLog extends ApiException {
    public static final String REQUIRED = "REQUIRED";
    public static final String NOT_ALLOWED = "NOT_ALLOWED";
    public static final String NOT_ALLOWED_FOR_DETAIL = "NOT_ALLOWED_FOR_DETAIL";
    public static final String WRONG_TYPE = "WRONG_TYPE";
    public static final String INVALID = "INVALID";
    public static final String RETIRED = "RETIRED";
    public static final String OUT_OF_RANGE = "OUT_OF_RANGE";
    public static final String TOO_LONG = "TOO_LONG";
    public static final String TOO_MANY = "TOO_MANY";
    public static final String DUPLICATE = "DUPLICATE";

    private final LogField field;
    private final String reason;

    public InvalidLog(LogField field, String reason) {
        super(field.getJsonName() + ": " + reason, "INVALID_LOG");
        this.field = field;
        this.reason = reason;
    }

    public LogField getField() {
        return this.field;
    }

    public String getReason() {
        return this.reason;
    }
}
