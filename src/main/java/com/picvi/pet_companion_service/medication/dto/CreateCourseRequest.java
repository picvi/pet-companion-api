package com.picvi.pet_companion_service.medication.dto;

import com.picvi.pet_companion_service.medication.domain.CourseInput;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code date} is the client's today; {@code clientId} makes a retried create safe. */
public record CreateCourseRequest(@NotNull UUID clientId,
                                  @NotNull LocalDate date,
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
                                  Boolean reminders) {

    public CourseInput toInput() {
        return new CourseInput(this.name, this.amountHundredths, this.unit, this.schedule, this.everyDays, this.times,
                this.withFood, this.startsOn, this.startMinute, this.endsOn, this.endsMinute);
    }

    public boolean remindersOn() {
        return this.reminders == null || this.reminders;
    }
}
