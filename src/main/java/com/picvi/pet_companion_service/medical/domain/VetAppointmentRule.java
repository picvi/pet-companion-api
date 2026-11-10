package com.picvi.pet_companion_service.medical.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class VetAppointmentRule {
    public static final int PASSED_VISIBLE_DAYS = 3;

    public Optional<NextVetAppointment> evaluate(List<PlannedAppointment> plans, LocalDate date) {
        return plans.stream()
                .filter(plan -> !plan.on().plusDays(PASSED_VISIBLE_DAYS).isBefore(date))
                .min(Comparator.comparing(PlannedAppointment::on).thenComparingInt(PlannedAppointment::startMinute))
                .map(plan -> new NextVetAppointment(plan.eventId(), plan.visitId(), plan.on(), plan.startMinute(), status(plan.on(), date)));
    }

    private static AppointmentStatus status(LocalDate on, LocalDate date) {
        if (on.isAfter(date)) {
            return AppointmentStatus.UPCOMING;
        }
        return on.isEqual(date) ? AppointmentStatus.TODAY : AppointmentStatus.PASSED;
    }
}
