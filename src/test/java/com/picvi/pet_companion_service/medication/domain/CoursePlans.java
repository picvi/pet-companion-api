package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;
import java.util.List;

public final class CoursePlans {
    private static final int YEAR = 2025;

    private CoursePlans() {
    }

    public static LocalDate oct(int day) {
        return LocalDate.of(YEAR, 10, day);
    }

    public static LocalDate nov(int day) {
        return LocalDate.of(YEAR, 11, day);
    }

    public static int at(int hour) {
        return hour * 60;
    }

    public static CoursePlan daily(List<Integer> times, LocalDate startsOn, int startMinute, LocalDate endsOn, Integer endsMinute) {
        return new CoursePlan(DoseSchedule.DAILY, null, times, startsOn, startMinute, endsOn, endsMinute, null, null);
    }

    public static CoursePlan everyDays(int days, List<Integer> times, LocalDate startsOn, int startMinute, LocalDate endsOn, Integer endsMinute) {
        return new CoursePlan(DoseSchedule.EVERY_N_DAYS, days, times, startsOn, startMinute, endsOn, endsMinute, null, null);
    }

    public static CoursePlan asNeeded(LocalDate startsOn) {
        return new CoursePlan(DoseSchedule.AS_NEEDED, null, List.of(), startsOn, 0, null, null, null, null);
    }

    public static CoursePlan stoppedAt(CoursePlan plan, LocalDate on, int minute) {
        return new CoursePlan(plan.schedule(), plan.everyDays(), plan.times(), plan.startsOn(), plan.startMinute(),
                plan.endsOn(), plan.endsMinute(), on, minute);
    }
}
