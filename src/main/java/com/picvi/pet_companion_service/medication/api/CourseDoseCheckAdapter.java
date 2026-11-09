package com.picvi.pet_companion_service.medication.api;

import com.picvi.pet_companion_service.exception.petlog.InvalidCourseDose;
import com.picvi.pet_companion_service.medication.domain.DoseTimesCalculator;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.petlog.api.CourseDoseCheck;
import com.picvi.pet_companion_service.petlog.domain.CourseDose;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseDoseCheckAdapter implements CourseDoseCheck {
    private final MedicationCourseRepository repository;
    private final DoseTimesCalculator calculator;

    @Override
    public void verify(UUID petId, CourseDose dose) {
        MedicationCourse course = this.repository.findByIdAndPetId(dose.courseId(), petId)
                .orElseThrow(() -> new InvalidCourseDose("courseId"));
        if (dose.answersPlannedTime() && !this.calculator.isPlanned(course.plan(), dose.plannedOn(), dose.plannedMinute())) {
            throw new InvalidCourseDose("plannedTime");
        }
    }
}
