package com.picvi.pet_companion_service.medical.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VetAppointmentRuleTest {
    private static final UUID VISIT_A = UUID.randomUUID();
    private static final UUID VISIT_B = UUID.randomUUID();
    private static final UUID EVENT_1 = UUID.randomUUID();
    private static final UUID EVENT_2 = UUID.randomUUID();

    private final VetAppointmentRule rule = new VetAppointmentRule();

    @Test
    void noAppointmentsGivesNothing() {
        assertThat(this.rule.evaluate(List.of(), List.of(), oct(16))).isEmpty();
    }

    @Test
    void anAppointmentBookedByAVisitIsUpcoming() {
        NextVetAppointment next = this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15)), 16).orElseThrow();

        assertThat(next.eventId()).isEqualTo(EVENT_1);
        assertThat(next.visitId()).isEqualTo(VISIT_A);
        assertThat(next.status()).isEqualTo(AppointmentStatus.UPCOMING);
    }

    @Test
    void onTheDayItIsToday() {
        assertThat(this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15)), 22).orElseThrow().status())
                .isEqualTo(AppointmentStatus.TODAY);
    }

    @Test
    void afterTheDayItStaysPassedForThreeDays() {
        assertThat(this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15)), 25).orElseThrow().status())
                .isEqualTo(AppointmentStatus.PASSED);
    }

    @Test
    void afterThreeDaysItDisappears() {
        assertThat(this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15)), 26)).isEmpty();
    }

    @Test
    void aVisitOnTheDayAttendsIt() {
        assertThat(this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15), visit(VISIT_B, 22)), 22)).isEmpty();
    }

    @Test
    void aVisitThreeDaysEarlyAttendsIt() {
        assertThat(this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15), visit(VISIT_B, 19)), 19)).isEmpty();
    }

    @Test
    void aVisitFourDaysEarlyDoesNot() {
        NextVetAppointment next = this.evaluate(List.of(appointment(EVENT_1, 22, 600, VISIT_A)), List.of(visit(VISIT_A, 15), visit(VISIT_B, 18)), 18)
                .orElseThrow();

        assertThat(next.status()).isEqualTo(AppointmentStatus.UPCOMING);
    }

    @Test
    void anAppointmentAddedInHerDayHasNoVisit() {
        NextVetAppointment next = this.evaluate(List.of(appointment(EVENT_1, 22, 600, null)), List.of(visit(VISIT_A, 15)), 16).orElseThrow();

        assertThat(next.eventId()).isEqualTo(EVENT_1);
        assertThat(next.visitId()).isNull();
    }

    @Test
    void aVisitAttendsAnAppointmentAddedInHerDay() {
        assertThat(this.evaluate(List.of(appointment(EVENT_1, 22, 600, null)), List.of(visit(VISIT_A, 20)), 20)).isEmpty();
    }

    @Test
    void theEarlierDateWinsOverAFarOneBookedByAVisit() {
        NextVetAppointment next = this.evaluate(
                List.of(appointment(EVENT_1, 22, 600, VISIT_A), appointment(EVENT_2, 22, 540, null)),
                List.of(visit(VISIT_A, 15)), 16).orElseThrow();

        assertThat(next.eventId()).isEqualTo(EVENT_2);
    }

    @Test
    void sameDayTheEarlierStartWins() {
        NextVetAppointment next = this.evaluate(
                List.of(appointment(EVENT_1, 22, 840, null), appointment(EVENT_2, 22, 540, null)), List.of(), 16).orElseThrow();

        assertThat(next.eventId()).isEqualTo(EVENT_2);
    }

    @Test
    void theBookingVisitNeverAttendsItsOwnAppointment() {
        NextVetAppointment next = this.evaluate(List.of(appointment(EVENT_1, 23, 600, VISIT_A)), List.of(visit(VISIT_A, 21)), 21).orElseThrow();

        assertThat(next.status()).isEqualTo(AppointmentStatus.UPCOMING);
    }

    private Optional<NextVetAppointment> evaluate(List<BookedAppointment> appointments, List<VisitDay> visits, int day) {
        return this.rule.evaluate(appointments, visits, oct(day));
    }

    private static BookedAppointment appointment(UUID eventId, int day, int minute, UUID bookedBy) {
        return new BookedAppointment(eventId, oct(day), minute, bookedBy);
    }

    private static VisitDay visit(UUID id, int day) {
        return new VisitDay(id, oct(day));
    }

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }
}
