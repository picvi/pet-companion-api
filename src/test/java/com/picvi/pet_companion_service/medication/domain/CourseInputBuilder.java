package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;
import java.util.List;

import static com.picvi.pet_companion_service.medication.domain.CoursePlans.at;

public class CourseInputBuilder {
    private String name = "Amoxicillin";
    private Integer amountHundredths = 100;
    private DoseSchedule schedule = DoseSchedule.DAILY;
    private Integer everyDays;
    private List<Integer> times = List.of(at(8), at(18));
    private LocalDate startsOn;
    private Integer startMinute = at(18);
    private LocalDate endsOn;
    private Integer endsMinute = at(18);

    public CourseInputBuilder(LocalDate today) {
        this.startsOn = today;
        this.endsOn = today.plusDays(7);
    }

    public CourseInputBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CourseInputBuilder amountHundredths(Integer amountHundredths) {
        this.amountHundredths = amountHundredths;
        return this;
    }

    public CourseInputBuilder schedule(DoseSchedule schedule) {
        this.schedule = schedule;
        return this;
    }

    public CourseInputBuilder everyDays(Integer everyDays) {
        this.everyDays = everyDays;
        return this;
    }

    public CourseInputBuilder times(List<Integer> times) {
        this.times = times;
        return this;
    }

    public CourseInputBuilder startsOn(LocalDate startsOn) {
        this.startsOn = startsOn;
        return this;
    }

    public CourseInputBuilder startMinute(Integer startMinute) {
        this.startMinute = startMinute;
        return this;
    }

    public CourseInputBuilder ends(LocalDate endsOn, Integer endsMinute) {
        this.endsOn = endsOn;
        this.endsMinute = endsMinute;
        return this;
    }

    public CourseInput build() {
        return new CourseInput(this.name, this.amountHundredths, DoseUnit.TABLET, this.schedule, this.everyDays, this.times,
                WithFood.YES, this.startsOn, this.startMinute, this.endsOn, this.endsMinute);
    }
}
