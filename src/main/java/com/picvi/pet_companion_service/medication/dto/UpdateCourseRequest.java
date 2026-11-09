package com.picvi.pet_companion_service.medication.dto;

import com.picvi.pet_companion_service.medication.domain.CourseInput;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** A full replace. The start is ignored once a dose was given. */
public record UpdateCourseRequest(@NotNull LocalDate date,
                                  String name,
                                  Integer amountHundredths,
                                  DoseUnit unit,
                                  DoseSchedule schedule,
                                  Integer everyDays,
                                  List<Integer> times,
                                  WithFood withFood,
                                  LocalDate startsOn,
                                  Integer startMinute,
                                  LocalDate endsOn,
                                  Integer endsMinute,
                                  Integer maxPerDay,
                                  Boolean reminders) {

    public CourseInput toInput() {
        return new CourseInput(this.name, this.amountHundredths, this.unit, this.schedule, this.everyDays, this.times,
                this.withFood, this.startsOn, this.startMinute, this.endsOn, this.endsMinute, this.maxPerDay);
    }

    public boolean remindersOn() {
        return this.reminders == null || this.reminders;
    }
}
