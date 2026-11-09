package com.picvi.pet_companion_service.medication.dto;

import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CourseDto(UUID id,
                        UUID clientId,
                        String name,
                        int amountHundredths,
                        DoseUnit unit,
                        DoseSchedule schedule,
                        Integer everyDays,
                        List<Integer> times,
                        WithFood withFood,
                        LocalDate startsOn,
                        int startMinute,
                        LocalDate endsOn,
                        Integer endsMinute,
                        Integer maxPerDay,
                        LocalDate stoppedOn,
                        Integer stoppedMinute,
                        boolean reminders,
                        CourseStatus status,
                        Integer dayNumber,
                        Integer totalDays,
                        int givenDoses,
                        LastGivenDto lastGiven,
                        List<PlannedDoseDto> doses,
                        long version) {

    private static final Comparator<PlannedDoseDto> DOSE_ORDER =
            Comparator.comparing(PlannedDoseDto::on).thenComparingInt(PlannedDoseDto::minute);

    /** Active courses first, then by the next dose still to give (as-needed courses have none), then by name. */
    public static final Comparator<CourseDto> LIST_ORDER = Comparator
            .comparing((CourseDto course) -> course.status() != CourseStatus.ACTIVE)
            .thenComparing(CourseDto::nextOpenDose, Comparator.nullsLast(DOSE_ORDER))
            .thenComparing(CourseDto::name);

    private PlannedDoseDto nextOpenDose() {
        return this.doses.stream().filter(dose -> dose.logId() == null).findFirst().orElse(null);
    }
}
