package com.picvi.pet_companion_service.medication.api;

import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.CourseStatusRule;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.medication.service.MedicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** What other modules may do with a pet's medicine courses. */
@Component
@RequiredArgsConstructor
public class MedicationApi {
    private final MedicationService service;
    private final MedicationCourseRepository repository;
    private final CourseStatusRule statusRule;

    /** A course as other modules see it. */
    public record CourseSummary(UUID id, String name, CourseStatus status) {
    }

    /** Starts a course, or returns the one already started with the same client id. */
    public UUID start(UUID ownerId, UUID petId, CreateCourseRequest request) {
        return this.service.create(ownerId, petId, request).course().id();
    }

    @Transactional(readOnly = true)
    public List<CourseSummary> courses(UUID petId, Collection<UUID> courseIds, LocalDate date) {
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return this.repository.findAllByPetIdAndIdIn(petId, courseIds).stream()
                .map(course -> summary(course, date))
                .toList();
    }

    public void stopOrDelete(UUID ownerId, UUID petId, UUID courseId, LocalDate date, int minuteOfDay) {
        this.service.stopOrDelete(ownerId, petId, courseId, date, minuteOfDay);
    }

    private CourseSummary summary(MedicationCourse course, LocalDate date) {
        return new CourseSummary(course.getId(), course.getName(), this.statusRule.status(course.plan(), date, 0));
    }
}
