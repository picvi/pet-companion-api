package com.picvi.pet_companion_service.medical.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VetAppointmentRuleTest {
    private static final UUID VISIT_A = UUID.randomUUID();
    private static final UUID VISIT_B = UUID.randomUUID();
    private static final UUID EVENT_1 = UUID.randomUUID();
    private static final UUID EVENT_2 = UUID.randomUUID();

    private final VetAppointmentRule rule = new VetAppointmentRule();

    @Test
    void noPlansGiveNothing() {
        assertThat(this.rule.evaluate(List.of(), oct(16))).isEmpty();
    }

    @Test
    void aPlanInTheFutureIsUpcomingAndPointsAtItsVisit() {
        NextVetAppointment next = this.rule.evaluate(List.of(plan(VISIT_A, EVENT_1, 22, 600)), oct(16)).orElseThrow();

        assertThat(next.eventId()).isEqualTo(EVENT_1);
        assertThat(next.visitId()).isEqualTo(VISIT_A);
        assertThat(next.status()).isEqualTo(AppointmentStatus.UPCOMING);
    }

    @Test
    void onTheDayItIsToday() {
        assertThat(this.rule.evaluate(List.of(plan(VISIT_A, EVENT_1, 22, 600)), oct(22)).orElseThrow().status())
                .isEqualTo(AppointmentStatus.TODAY);
    }

    @Test
    void aPassedPlanStaysVisibleForThreeDays() {
        assertThat(this.rule.evaluate(List.of(plan(VISIT_A, EVENT_1, 22, 600)), oct(25)).orElseThrow().status())
                .isEqualTo(AppointmentStatus.PASSED);
        assertThat(this.rule.evaluate(List.of(plan(VISIT_A, EVENT_1, 22, 600)), oct(26))).isEmpty();
    }

    @Test
    void theEarliestDayThenTheEarliestMinuteWins() {
        List<PlannedAppointment> plans = List.of(
                plan(VISIT_A, EVENT_1, 24, 540),
                plan(VISIT_B, EVENT_2, 23, 900),
                plan(UUID.randomUUID(), UUID.randomUUID(), 23, 960));

        assertThat(this.rule.evaluate(plans, oct(16)).orElseThrow().visitId()).isEqualTo(VISIT_B);
    }

    private static PlannedAppointment plan(UUID visitId, UUID eventId, int day, int minute) {
        return new PlannedAppointment(visitId, eventId, oct(day), minute);
    }

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }
}
