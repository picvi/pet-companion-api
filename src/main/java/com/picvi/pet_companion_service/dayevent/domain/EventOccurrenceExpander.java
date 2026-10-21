package com.picvi.pet_companion_service.dayevent.domain;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class EventOccurrenceExpander {

    public List<EventOccurrence> expand(List<EventDefinition> events, List<OccurrenceChange> changes, LocalDate date) {
        return events.stream()
                .filter(event -> this.occursOn(event, date))
                .map(event -> this.occurrence(event, this.changeFor(changes, event, date)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingInt(EventOccurrence::startMinute))
                .toList();
    }

    private boolean occursOn(EventDefinition event, LocalDate date) {
        if (event.recurrence() == Recurrence.WEEKLY) {
            return !date.isBefore(event.firstDate()) && date.getDayOfWeek() == event.firstDate().getDayOfWeek();
        }
        return event.firstDate().equals(date);
    }

    private Optional<OccurrenceChange> changeFor(List<OccurrenceChange> changes, EventDefinition event, LocalDate date) {
        return changes.stream()
                .filter(change -> change.eventId().equals(event.id()) && change.date().equals(date))
                .findFirst();
    }

    private Optional<EventOccurrence> occurrence(EventDefinition event, Optional<OccurrenceChange> change) {
        if (change.map(OccurrenceChange::skipped).orElse(false)) {
            return Optional.empty();
        }
        int startMinute = change.map(OccurrenceChange::startMinute).orElse(event.startMinute());
        return Optional.of(new EventOccurrence(event.id(), event.type(), event.title(), startMinute,
                event.durationMinutes(), event.recurrence(), event.planAround()));
    }
}
