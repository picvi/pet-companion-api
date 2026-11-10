package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record CoursePlan(DoseSchedule schedule, Integer everyDays, List<Integer> times,
                         LocalDate startsOn, int startMinute,
                         LocalDate endsOn, Integer endsMinute,
                         LocalDate stoppedOn, Integer stoppedMinute) {
    private static final int MINUTES_PER_DAY = 1440;

    public CoursePlan {
        times = times.stream().sorted(Comparator.naturalOrder()).toList();
    }

    public int stepDays() {
        return this.schedule == DoseSchedule.EVERY_N_DAYS ? this.everyDays : 1;
    }

    public boolean hasEnd() {
        return this.endsOn != null;
    }

    public boolean isStopped() {
        return this.stoppedOn != null;
    }

    public boolean endedBy(LocalDate on, int minute) {
        return this.hasEnd() && moment(on, minute) >= moment(this.endsOn, this.endsMinute);
    }

    public boolean includes(LocalDate on, int minute) {
        long moment = moment(on, minute);
        boolean started = moment >= moment(this.startsOn, this.startMinute);
        boolean stopped = this.isStopped() && moment > moment(this.stoppedOn, this.stoppedMinute);
        return started && !stopped && !this.endedBy(on, minute);
    }

    private static long moment(LocalDate on, int minute) {
        return on.toEpochDay() * MINUTES_PER_DAY + minute;
    }
}
