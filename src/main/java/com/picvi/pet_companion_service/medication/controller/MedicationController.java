package com.picvi.pet_companion_service.medication.controller;

import com.picvi.pet_companion_service.exception.medication.InvalidCourse;
import com.picvi.pet_companion_service.medication.dto.CourseDto;
import com.picvi.pet_companion_service.medication.dto.CoursesResponse;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import com.picvi.pet_companion_service.medication.dto.StopCourseRequest;
import com.picvi.pet_companion_service.medication.dto.UpdateCourseRequest;
import com.picvi.pet_companion_service.medication.service.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pets/{petId}/medications")
public class MedicationController {
    private final MedicationService medicationService;

    @GetMapping
    public CoursesResponse list(@AuthenticationPrincipal UUID userId,
                                @PathVariable UUID petId,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.medicationService.list(userId, petId, date);
    }

    @GetMapping("/{courseId}")
    public CourseDto get(@AuthenticationPrincipal UUID userId,
                         @PathVariable UUID petId,
                         @PathVariable UUID courseId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return this.medicationService.get(userId, petId, courseId, date);
    }

    @PostMapping
    public ResponseEntity<CourseDto> create(@AuthenticationPrincipal UUID userId,
                                            @PathVariable UUID petId,
                                            @Valid @RequestBody CreateCourseRequest request) {
        MedicationService.CreateResult result = this.medicationService.create(userId, petId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.course());
    }

    @PutMapping("/{courseId}")
    public CourseDto update(@AuthenticationPrincipal UUID userId,
                            @PathVariable UUID petId,
                            @PathVariable UUID courseId,
                            @RequestHeader("If-Match") String ifMatch,
                            @Valid @RequestBody UpdateCourseRequest request) {
        return this.medicationService.update(userId, petId, courseId, parseVersion(ifMatch), request);
    }

    @PostMapping("/{courseId}/stop")
    public CourseDto stop(@AuthenticationPrincipal UUID userId,
                          @PathVariable UUID petId,
                          @PathVariable UUID courseId,
                          @Valid @RequestBody StopCourseRequest request) {
        return this.medicationService.stop(userId, petId, courseId, request);
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UUID userId,
                       @PathVariable UUID petId,
                       @PathVariable UUID courseId) {
        this.medicationService.delete(userId, petId, courseId);
    }

    private static long parseVersion(String ifMatch) {
        try {
            return Long.parseLong(ifMatch.replace("W/", "").replace("\"", "").trim());
        } catch (NumberFormatException e) {
            throw new InvalidCourse("version", InvalidCourse.INVALID);
        }
    }
}
