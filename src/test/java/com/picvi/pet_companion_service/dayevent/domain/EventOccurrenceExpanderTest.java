package com.picvi.pet_companion_service.dayevent.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventOccurrenceExpanderTest {
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 10, 14);

    private final EventOccurrenceExpander expander = new EventOccurrenceExpander();

    @Test
    void aOneOffEventOccursOnlyOnItsDate() {
        EventDefinition vet = event(DayEventType.VET, WEDNESDAY, 600, Recurrence.NONE);

        assertThat(this.expander.expand(List.of(vet), List.of(), WEDNESDAY)).extracting(EventOccurrence::eventId).containsExactly(vet.id());
        assertThat(this.expander.expand(List.of(vet), List.of(), WEDNESDAY.plusDays(1))).isEmpty();
        assertThat(this.expander.expand(List.of(vet), List.of(), WEDNESDAY.minusDays(1))).isEmpty();
    }

    @Test
    void aWeeklyEventRepeatsOnTheSameWeekdayFromItsFirstDate() {
        EventDefinition puppyClass = event(DayEventType.CLASS, WEDNESDAY, 1000, Recurrence.WEEKLY);

        assertThat(this.expander.expand(List.of(puppyClass), List.of(), WEDNESDAY.plusWeeks(1))).hasSize(1);
        assertThat(this.expander.expand(List.of(puppyClass), List.of(), WEDNESDAY.plusWeeks(5))).hasSize(1);
        assertThat(this.expander.expand(List.of(puppyClass), List.of(), WEDNESDAY.plusDays(3))).isEmpty();
        assertThat(this.expander.expand(List.of(puppyClass), List.of(), WEDNESDAY.minusWeeks(1))).isEmpty();
    }

    @Test
    void aSkippedOccurrenceIsLeftOutButOtherWeeksStay() {
        EventDefinition puppyClass = event(DayEventType.CLASS, WEDNESDAY, 1000, Recurrence.WEEKLY);
        OccurrenceChange skip = new OccurrenceChange(puppyClass.id(), WEDNESDAY.plusWeeks(1), true, null);

        assertThat(this.expander.expand(List.of(puppyClass), List.of(skip), WEDNESDAY.plusWeeks(1))).isEmpty();
        assertThat(this.expander.expand(List.of(puppyClass), List.of(skip), WEDNESDAY.plusWeeks(2))).hasSize(1);
    }

    @Test
    void aMovedOccurrenceUsesTheNewStartOnlyOnThatDay() {
        EventDefinition puppyClass = event(DayEventType.CLASS, WEDNESDAY, 1000, Recurrence.WEEKLY);
        OccurrenceChange move = new OccurrenceChange(puppyClass.id(), WEDNESDAY.plusWeeks(1), false, 1100);

        assertThat(this.expander.expand(List.of(puppyClass), List.of(move), WEDNESDAY.plusWeeks(1)))
                .extracting(EventOccurrence::startMinute).containsExactly(1100);
        assertThat(this.expander.expand(List.of(puppyClass), List.of(move), WEDNESDAY.plusWeeks(2)))
                .extracting(EventOccurrence::startMinute).containsExactly(1000);
    }

    @Test
    void occurrencesComeInTimeOrder() {
        EventDefinition late = event(DayEventType.GUESTS, WEDNESDAY, 1000, Recurrence.NONE);
        EventDefinition early = event(DayEventType.VET, WEDNESDAY, 600, Recurrence.NONE);

        assertThat(this.expander.expand(List.of(late, early), List.of(), WEDNESDAY))
                .extracting(EventOccurrence::startMinute).containsExactly(600, 1000);
    }

    private static EventDefinition event(DayEventType type, LocalDate firstDate, int startMinute, Recurrence recurrence) {
        return new EventDefinition(UUID.randomUUID(), type, null, firstDate, startMinute, 60, recurrence, true);
    }
}
