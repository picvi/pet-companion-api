package com.picvi.pet_companion_service.exception.medication;

import com.picvi.pet_companion_service.exception.ApiException;

public class CourseHasDoses extends ApiException {
    public CourseHasDoses() {
        super("Doses were already given from this course, stop it instead of deleting", "COURSE_HAS_DOSES");
    }
}
