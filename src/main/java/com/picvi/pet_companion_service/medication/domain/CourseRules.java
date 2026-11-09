package com.picvi.pet_companion_service.medication.domain;

import com.picvi.pet_companion_service.exception.medication.InvalidCourse;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;

public class CourseRules {
    public static final int MAX_NAME_LENGTH = 80;
    public static final int MAX_AMOUNT_HUNDREDTHS = 100000;
    public static final int MAX_TIMES_PER_DAY = 6;
    public static final int MIN_EVERY_DAYS = 2;
    public static final int MAX_EVERY_DAYS = 14;
    public static final int MAX_AS_NEEDED_PER_DAY = 6;
    public static final int START_WINDOW_DAYS = 30;
    public static final int MAX_COURSE_DAYS = 365;
    private static final int LAST_MINUTE = 1439;

    public void validate(CourseInput input, LocalDate clientDate) {
        this.validateStartedCourse(input);
        this.validateStartWindow(input.startsOn(), clientDate);
    }

    public void validateStartedCourse(CourseInput input) {
        this.validateName(input.name());
        this.validateAmount(input.amountHundredths());
        this.require("unit", input.unit());
        this.require("withFood", input.withFood());
        this.require("schedule", input.schedule());
        this.validateSchedule(input);
        this.validateStart(input);
        this.validateEnd(input);
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidCourse("name", InvalidCourse.REQUIRED);
        }
        if (name.trim().length() > MAX_NAME_LENGTH) {
            throw new InvalidCourse("name", InvalidCourse.TOO_LONG);
        }
    }

    private void validateAmount(Integer amountHundredths) {
        this.require("amountHundredths", amountHundredths);
        if (amountHundredths < 1 || amountHundredths > MAX_AMOUNT_HUNDREDTHS) {
            throw new InvalidCourse("amountHundredths", InvalidCourse.OUT_OF_RANGE);
        }
    }

    private void validateSchedule(CourseInput input) {
        boolean asNeeded = input.schedule() == DoseSchedule.AS_NEEDED;
        if (asNeeded) {
            this.validateAsNeeded(input);
        } else {
            this.validateTimes(input.times());
        }
        if (input.schedule() == DoseSchedule.EVERY_N_DAYS) {
            this.require("everyDays", input.everyDays());
            if (input.everyDays() < MIN_EVERY_DAYS || input.everyDays() > MAX_EVERY_DAYS) {
                throw new InvalidCourse("everyDays", InvalidCourse.OUT_OF_RANGE);
            }
        } else if (input.everyDays() != null) {
            throw new InvalidCourse("everyDays", InvalidCourse.INVALID);
        }
    }

    private void validateAsNeeded(CourseInput input) {
        if (input.times() != null && !input.times().isEmpty()) {
            throw new InvalidCourse("times", InvalidCourse.INVALID);
        }
        Integer max = input.maxPerDay();
        if (max != null && (max < 1 || max > MAX_AS_NEEDED_PER_DAY)) {
            throw new InvalidCourse("maxPerDay", InvalidCourse.OUT_OF_RANGE);
        }
    }

    private void validateTimes(List<Integer> times) {
        if (times == null || times.isEmpty()) {
            throw new InvalidCourse("times", InvalidCourse.REQUIRED);
        }
        if (times.size() > MAX_TIMES_PER_DAY) {
            throw new InvalidCourse("times", InvalidCourse.OUT_OF_RANGE);
        }
        HashSet<Integer> distinct = new HashSet<>();
        for (Integer minute : times) {
            if (minute == null || !distinct.add(minute)) {
                throw new InvalidCourse("times", InvalidCourse.INVALID);
            }
            if (minute < 0 || minute > LAST_MINUTE) {
                throw new InvalidCourse("times", InvalidCourse.OUT_OF_RANGE);
            }
        }
    }

    private void validateStart(CourseInput input) {
        this.require("startsOn", input.startsOn());
        this.require("startMinute", input.startMinute());
        if (input.startMinute() < 0 || input.startMinute() > LAST_MINUTE) {
            throw new InvalidCourse("startMinute", InvalidCourse.OUT_OF_RANGE);
        }
    }

    private void validateStartWindow(LocalDate startsOn, LocalDate clientDate) {
        if (startsOn.isBefore(clientDate.minusDays(START_WINDOW_DAYS)) || startsOn.isAfter(clientDate.plusDays(START_WINDOW_DAYS))) {
            throw new InvalidCourse("startsOn", InvalidCourse.OUT_OF_RANGE);
        }
    }

    private void validateEnd(CourseInput input) {
        if (input.endsOn() == null && input.endsMinute() == null) {
            return;
        }
        this.require("endsOn", input.endsOn());
        this.require("endsMinute", input.endsMinute());
        if (input.endsMinute() < 0 || input.endsMinute() > LAST_MINUTE) {
            throw new InvalidCourse("endsMinute", InvalidCourse.OUT_OF_RANGE);
        }
        CoursePlan plan = input.plan();
        if (plan.endedBy(input.startsOn(), input.startMinute())) {
            throw new InvalidCourse("endsOn", InvalidCourse.OUT_OF_RANGE);
        }
        if (input.endsOn().isAfter(input.startsOn().plusDays(MAX_COURSE_DAYS))) {
            throw new InvalidCourse("endsOn", InvalidCourse.OUT_OF_RANGE);
        }
    }

    private void require(String field, Object value) {
        if (value == null) {
            throw new InvalidCourse(field, InvalidCourse.REQUIRED);
        }
    }
}
