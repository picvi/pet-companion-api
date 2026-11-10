package com.picvi.pet_companion_service.medication.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.medication.CourseHasDoses;
import com.picvi.pet_companion_service.exception.medication.CourseNotFound;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medication.domain.CourseInput;
import com.picvi.pet_companion_service.medication.domain.CourseRules;
import com.picvi.pet_companion_service.medication.dto.CourseDto;
import com.picvi.pet_companion_service.medication.dto.CoursesResponse;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import com.picvi.pet_companion_service.medication.dto.StopCourseRequest;
import com.picvi.pet_companion_service.medication.dto.UpdateCourseRequest;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class MedicationService {
    static final int FINISHED_VISIBLE_DAYS = 7;

    public record CreateResult(CourseDto course, boolean created) {
    }

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final MedicationCourseRepository repository;
    private final CourseRules rules;
    private final CourseAssembler assembler;
    private final PetLogApi petLogApi;
    private final Clock clock;

    @Transactional(readOnly = true)
    public CoursesResponse list(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        List<MedicationCourse> courses = this.repository.findNotEndedBefore(petId, date.minusDays(FINISHED_VISIBLE_DAYS));
        List<CourseDto> dtos = this.assembler.assemble(courses, date, date.minusDays(1)).stream()
                .sorted(CourseDto.LIST_ORDER)
                .toList();
        return new CoursesResponse(date, dtos);
    }

    @Transactional(readOnly = true)
    public CourseDto get(UUID ownerId, UUID petId, UUID courseId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        MedicationCourse course = this.repository.findByIdAndPetId(courseId, petId).orElseThrow(CourseNotFound::new);
        return this.assembler.assemble(List.of(course), date, course.getStartsOn()).getFirst();
    }

    public CreateResult create(UUID ownerId, UUID petId, CreateCourseRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        Optional<MedicationCourse> existing = this.repository.findByPetIdAndClientId(petId, request.clientId());
        if (existing.isPresent()) {
            return new CreateResult(this.view(existing.get(), request.date()), false);
        }
        CourseInput input = request.toInput();
        this.rules.validate(input, request.date());
        MedicationCourse saved = this.repository.saveAndFlush(
                new MedicationCourse(petId, request.clientId(), input, request.remindersOn(), this.clock.instant()));
        return new CreateResult(this.view(saved, request.date()), true);
    }

    public CourseDto update(UUID ownerId, UUID petId, UUID courseId, long expectedVersion, UpdateCourseRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        MedicationCourse course = this.repository.findByIdAndPetId(courseId, petId).orElseThrow(CourseNotFound::new);
        if (course.getVersion() != expectedVersion) {
            throw new VersionConflict();
        }
        CourseInput input = request.toInput();
        if (this.hasDoses(courseId)) {
            input = input.startingAt(course.getStartsOn(), course.getStartMinute());
            this.rules.validateStartedCourse(input);
        } else {
            this.rules.validate(input, request.date());
        }
        course.replace(input, request.remindersOn(), this.clock.instant());
        return this.view(this.repository.saveAndFlush(course), request.date());
    }

    public CourseDto stop(UUID ownerId, UUID petId, UUID courseId, StopCourseRequest request) {
        this.clientDateValidator.validate(request.date());
        this.petService.getOwnedPetProfile(ownerId, petId);
        MedicationCourse course = this.repository.findByIdAndPetId(courseId, petId).orElseThrow(CourseNotFound::new);
        course.stop(request.date(), request.minuteOfDay(), this.clock.instant());
        return this.view(this.repository.saveAndFlush(course), request.date());
    }

    public void delete(UUID ownerId, UUID petId, UUID courseId) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.repository.findByIdAndPetId(courseId, petId).ifPresent(course -> {
            if (this.hasDoses(courseId)) {
                throw new CourseHasDoses();
            }
            this.repository.delete(course);
        });
    }

    /** A course nobody has given a dose of disappears; one that has history is only stopped. Unknown courses are ignored. */
    public void stopOrDelete(UUID ownerId, UUID petId, UUID courseId, LocalDate date, int minuteOfDay) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        this.repository.findByIdAndPetId(courseId, petId).ifPresent(course -> {
            if (this.hasDoses(courseId)) {
                course.stop(date, minuteOfDay, this.clock.instant());
                return;
            }
            this.repository.delete(course);
        });
    }

    private boolean hasDoses(UUID courseId) {
        return this.petLogApi.courseDoseTotals(List.of(courseId)).containsKey(courseId);
    }

    private CourseDto view(MedicationCourse course, LocalDate date) {
        return this.assembler.assemble(List.of(course), date, course.getStartsOn()).getFirst();
    }
}
