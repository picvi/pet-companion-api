package com.picvi.pet_companion_service.medical.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class VetAppointmentRule {
    public static final int ATTENDED_EARLY_DAYS = 3;
    public static final int PASSED_VISIBLE_DAYS = 3;

    public Optional<NextVetAppointment> evaluate(List<BookedAppointment> appointments, List<VisitDay> visits, LocalDate date) {
        return appointments.stream()
                .filter(appointment -> !appointment.on().plusDays(PASSED_VISIBLE_DAYS).isBefore(date))
                .filter(appointment -> !this.attended(appointment, visits))
                .min(Comparator.comparing(BookedAppointment::on).thenComparingInt(BookedAppointment::startMinute))
                .map(appointment -> new NextVetAppointment(appointment.eventId(), appointment.bookedByVisitId(), appointment.on(),
                        appointment.startMinute(), status(appointment.on(), date)));
    }

    private boolean attended(BookedAppointment appointment, List<VisitDay> visits) {
        LocalDate earliest = appointment.on().minusDays(ATTENDED_EARLY_DAYS);
        return visits.stream()
                .filter(visit -> !visit.visitId().equals(appointment.bookedByVisitId()))
                .anyMatch(visit -> !visit.visitedOn().isBefore(earliest));
    }

    private static AppointmentStatus status(LocalDate on, LocalDate date) {
        if (on.isAfter(date)) {
            return AppointmentStatus.UPCOMING;
        }
        return on.isEqual(date) ? AppointmentStatus.TODAY : AppointmentStatus.PASSED;
    }
}
