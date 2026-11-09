package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidLog;

import java.time.LocalDate;
import java.util.UUID;

/** The course a medicine log belongs to and, for a planned dose, the time it answered. */
public record CourseDose(UUID courseId, LocalDate plannedOn, Integer plannedMinute) {

    public static CourseDose of(UUID courseId, LocalDate plannedOn, Integer plannedMinute) {
        boolean partialPlannedTime = (plannedOn == null) != (plannedMinute == null);
        if (partialPlannedTime || (courseId == null && plannedOn != null)) {
            throw new InvalidLog(LogField.COURSE_DOSE, InvalidLog.INVALID);
        }
        return courseId == null ? null : new CourseDose(courseId, plannedOn, plannedMinute);
    }

    public boolean answersPlannedTime() {
        return this.plannedOn != null;
    }
}
