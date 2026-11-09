package com.picvi.pet_companion_service.exception.medication;

import com.picvi.pet_companion_service.exception.ApiException;

public class CourseNotFound extends ApiException {
    public CourseNotFound() {
        super("This medicine course was not found", "COURSE_NOT_FOUND");
    }
}
