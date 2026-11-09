package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;

public class CourseStatusRule {
    public CourseStatus status(CoursePlan plan, LocalDate date, int minute) {
        if (plan.isStopped()) {
            return CourseStatus.STOPPED;
        }
        return plan.endedBy(date, minute) ? CourseStatus.FINISHED : CourseStatus.ACTIVE;
    }
}
