package com.picvi.pet_companion_service.exception.petlog;

import com.picvi.pet_companion_service.exception.ApiException;

public class InvalidCourseDose extends ApiException {
    public InvalidCourseDose(String field) {
        super(field + ": this dose does not belong to the course", "INVALID_COURSE_DOSE");
    }
}
