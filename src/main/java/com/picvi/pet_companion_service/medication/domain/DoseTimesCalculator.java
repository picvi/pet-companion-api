package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class DoseTimesCalculator {
    public static final int MAX_DOSES = 400;
    private static final int LAST_DOSE_SEARCH_DAYS = 28;

    public List<PlannedDose> between(CoursePlan plan, LocalDate from, LocalDate to) {
        if (plan.schedule() == DoseSchedule.AS_NEEDED) {
            return List.of();
        }
        List<PlannedDose> doses = new ArrayList<>();
        LocalDate lastDay = this.lastDayToConsider(plan, to);
        for (LocalDate day = this.firstDoseDay(plan, from); !day.isAfter(lastDay); day = day.plusDays(plan.stepDays())) {
            for (int minute : plan.times()) {
                if (plan.includes(day, minute)) {
                    doses.add(new PlannedDose(day, minute));
                }
                if (doses.size() == MAX_DOSES) {
                    return doses;
                }
            }
        }
        return doses;
    }

    public boolean isPlanned(CoursePlan plan, LocalDate on, int minute) {
        if (plan.schedule() == DoseSchedule.AS_NEEDED || !plan.times().contains(minute)) {
            return false;
        }
        long daysSinceStart = ChronoUnit.DAYS.between(plan.startsOn(), on);
        return daysSinceStart >= 0 && daysSinceStart % plan.stepDays() == 0 && plan.includes(on, minute);
    }

    public Optional<PlannedDose> last(CoursePlan plan) {
        if (!plan.hasEnd() && !plan.isStopped()) {
            return Optional.empty();
        }
        LocalDate lastDay = this.lastDayToConsider(plan, LocalDate.MAX);
        LocalDate from = lastDay.minusDays(LAST_DOSE_SEARCH_DAYS);
        List<PlannedDose> doses = this.between(plan, from.isBefore(plan.startsOn()) ? plan.startsOn() : from, lastDay);
        return doses.isEmpty() ? Optional.empty() : Optional.of(doses.get(doses.size() - 1));
    }

    public OptionalInt dayNumber(CoursePlan plan, LocalDate date) {
        if (!plan.hasEnd() || date.isBefore(plan.startsOn()) || !date.isBefore(plan.endsOn())) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) ChronoUnit.DAYS.between(plan.startsOn(), date) + 1);
    }

    public OptionalInt totalDays(CoursePlan plan) {
        if (!plan.hasEnd()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of((int) ChronoUnit.DAYS.between(plan.startsOn(), plan.endsOn()));
    }

    private LocalDate firstDoseDay(CoursePlan plan, LocalDate from) {
        if (!from.isAfter(plan.startsOn())) {
            return plan.startsOn();
        }
        long step = plan.stepDays();
        long daysSinceStart = ChronoUnit.DAYS.between(plan.startsOn(), from);
        long steps = (daysSinceStart + step - 1) / step;
        return plan.startsOn().plusDays(steps * step);
    }

    private LocalDate lastDayToConsider(CoursePlan plan, LocalDate to) {
        LocalDate last = to;
        if (plan.hasEnd() && plan.endsOn().isBefore(last)) {
            last = plan.endsOn();
        }
        if (plan.isStopped() && plan.stoppedOn().isBefore(last)) {
            last = plan.stoppedOn();
        }
        return last;
    }
}
