package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;
import java.util.List;

public record CourseInput(String name, Integer amountHundredths, DoseUnit unit, DoseSchedule schedule,
                          Integer everyDays, List<Integer> times, WithFood withFood,
                          LocalDate startsOn, Integer startMinute,
                          LocalDate endsOn, Integer endsMinute) {

    public CourseInput startingAt(LocalDate startsOn, Integer startMinute) {
        return new CourseInput(this.name, this.amountHundredths, this.unit, this.schedule, this.everyDays, this.times,
                this.withFood, startsOn, startMinute, this.endsOn, this.endsMinute);
    }

    public CoursePlan plan() {
        return new CoursePlan(this.schedule, this.everyDays, this.times == null ? List.of() : this.times, this.startsOn, this.startMinute,
                this.endsOn, this.endsMinute, null, null);
    }
}
