package com.picvi.pet_companion_service.medication.api;

import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.CourseStatusRule;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.medication.service.MedicationService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** What other modules may do with a pet's medicine courses. */
@Component
@RequiredArgsConstructor
public class MedicationApi {
    private final MedicationService service;
    private final MedicationCourseRepository repository;
    private final CourseStatusRule statusRule;
    private final PetLogApi petLogApi;

    /** A course as other modules see it. */
    public record CourseSummary(UUID id, String name, CourseStatus status) {
    }

    /** A course that ran during a period, with how many doses were given in all. */
    public record CourseInPeriod(String name, int amountHundredths, DoseUnit unit, DoseSchedule schedule, CourseStatus status,
                                 LocalDate startsOn, LocalDate endsOn, int givenDoses) {
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

    /** Courses whose time overlaps the period, both days included, earliest start first. */
    @Transactional(readOnly = true)
    public List<CourseInPeriod> coursesBetween(UUID petId, LocalDate from, LocalDate to, LocalDate date) {
        List<MedicationCourse> courses = this.repository.findOverlapping(petId, from, to);
        Map<UUID, PetLogApi.CourseDoseTotals> totals = this.petLogApi.courseDoseTotals(courses.stream().map(MedicationCourse::getId).toList());
        return courses.stream()
                .map(course -> new CourseInPeriod(course.getName(), course.getAmountHundredths(), course.getUnit(), course.getSchedule(),
                        this.statusRule.status(course.plan(), date, 0), course.getStartsOn(), course.getEndsOn(),
                        totals.containsKey(course.getId()) ? totals.get(course.getId()).given() : 0))
                .toList();
    }

    public void stopOrDelete(UUID ownerId, UUID petId, UUID courseId, LocalDate date, int minuteOfDay) {
        this.service.stopOrDelete(ownerId, petId, courseId, date, minuteOfDay);
    }

    private CourseSummary summary(MedicationCourse course, LocalDate date) {
        return new CourseSummary(course.getId(), course.getName(), this.statusRule.status(course.plan(), date, 0));
    }
}
