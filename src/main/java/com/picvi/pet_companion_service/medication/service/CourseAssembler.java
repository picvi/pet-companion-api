package com.picvi.pet_companion_service.medication.service;

import com.picvi.pet_companion_service.medication.domain.CoursePlan;
import com.picvi.pet_companion_service.medication.domain.CourseStatusRule;
import com.picvi.pet_companion_service.medication.domain.DoseTimesCalculator;
import com.picvi.pet_companion_service.medication.domain.PlannedDose;
import com.picvi.pet_companion_service.medication.dto.CourseDto;
import com.picvi.pet_companion_service.medication.dto.LastGivenDto;
import com.picvi.pet_companion_service.medication.dto.PlannedDoseDto;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.CourseDoseGiven;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.CourseDoseTotals;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Turns stored courses into what the app shows: status, planned doses with who gave them, and totals. */
@Component
@RequiredArgsConstructor
class CourseAssembler {
    private static final int DAYS_AHEAD = 2;

    private final DoseTimesCalculator calculator;
    private final CourseStatusRule statusRule;
    private final PetLogApi petLogApi;

    List<CourseDto> assemble(List<MedicationCourse> courses, LocalDate date, LocalDate dosesFrom) {
        List<UUID> ids = courses.stream().map(MedicationCourse::getId).toList();
        LocalDate dosesTo = date.plusDays(DAYS_AHEAD);
        Map<UUID, List<CourseDoseGiven>> given = this.petLogApi.courseDosesBetween(ids, dosesFrom, dosesTo);
        Map<UUID, CourseDoseTotals> totals = this.petLogApi.courseDoseTotals(ids);
        return courses.stream()
                .map(course -> this.assemble(course, date, dosesFrom, dosesTo, given.getOrDefault(course.getId(), List.of()), totals.get(course.getId())))
                .toList();
    }

    private CourseDto assemble(MedicationCourse course, LocalDate date, LocalDate dosesFrom, LocalDate dosesTo,
                               List<CourseDoseGiven> given, CourseDoseTotals totals) {
        CoursePlan plan = course.plan();
        return new CourseDto(course.getId(), course.getClientId(), course.getName(), course.getAmountHundredths(), course.getUnit(),
                course.getSchedule(), course.getEveryDays(), course.getTimes().stream().sorted().toList(), course.getWithFood(),
                course.getStartsOn(), course.getStartMinute(), course.getEndsOn(), course.getEndsMinute(), course.getMaxPerDay(),
                course.getStoppedOn(), course.getStoppedMinute(), course.isReminders(),
                this.statusRule.status(plan, date, 0),
                this.calculator.dayNumber(plan, date).stream().boxed().findFirst().orElse(null),
                this.calculator.totalDays(plan).stream().boxed().findFirst().orElse(null),
                totals == null ? 0 : totals.given(),
                totals == null ? null : new LastGivenDto(totals.lastOn(), totals.lastMinute()),
                this.plannedDoses(plan, dosesFrom, dosesTo, given),
                course.getVersion());
    }

    private List<PlannedDoseDto> plannedDoses(CoursePlan plan, LocalDate from, LocalDate to, List<CourseDoseGiven> given) {
        Map<PlannedDose, UUID> logIds = given.stream()
                .collect(Collectors.toMap(dose -> new PlannedDose(dose.plannedOn(), dose.plannedMinute()), CourseDoseGiven::logId));
        return this.calculator.between(plan, from, to).stream()
                .map(dose -> new PlannedDoseDto(dose.on(), dose.minute(), logIds.get(dose)))
                .toList();
    }
}
