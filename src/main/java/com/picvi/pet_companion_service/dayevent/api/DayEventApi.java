package com.picvi.pet_companion_service.dayevent.api;

import com.picvi.pet_companion_service.dayevent.domain.DayEventRules;
import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEvent;
import com.picvi.pet_companion_service.dayevent.repository.PetDayEventRepository;
import com.picvi.pet_companion_service.dayevent.service.DayEventService;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What other modules may ask of or do to a pet's day events. Ownership is checked by the caller. */
@Component
@RequiredArgsConstructor
public class DayEventApi {
    public static final int VET_APPOINTMENT_MINUTES = 60;

    private final PetService petService;
    private final DayEventRules eventRules;
    private final DayEventService eventService;
    private final PetDayEventRepository eventRepository;

    /** A one-off vet appointment as it stands in her day. */
    public record VetAppointment(UUID eventId, LocalDate on, int startMinute) {
    }

    @Transactional
    public UUID scheduleVet(UUID petId, LocalDate on, int startMinute, LocalDate clientDate) {
        this.eventRules.validate(startMinute, VET_APPOINTMENT_MINUTES, null, on, clientDate);
        PetDayEvent event = new PetDayEvent(this.petService.getReference(petId));
        event.setType(DayEventType.VET);
        event.setTitle(null);
        event.setLocalDate(on);
        event.setStartMinute(startMinute);
        event.setDurationMinutes(VET_APPOINTMENT_MINUTES);
        event.setRecurrence(Recurrence.NONE);
        event.setPlanAround(true);
        return this.eventRepository.save(event).getId();
    }

    @Transactional
    public void rescheduleVet(UUID petId, UUID eventId, LocalDate on, int startMinute, LocalDate clientDate) {
        this.eventRepository.findByIdAndPetId(eventId, petId).ifPresent(event -> {
            this.eventRules.validate(startMinute, VET_APPOINTMENT_MINUTES, null, on, clientDate);
            event.setLocalDate(on);
            event.setStartMinute(startMinute);
            this.eventRepository.save(event);
        });
    }

    @Transactional
    public void cancel(UUID petId, UUID eventId) {
        this.eventRepository.findByIdAndPetId(eventId, petId).ifPresent(this.eventRepository::delete);
    }

    @Transactional(readOnly = true)
    public Map<UUID, VetAppointment> vetAppointments(UUID petId, Collection<UUID> eventIds) {
        return this.eventRepository.findAllById(eventIds).stream()
                .filter(event -> event.getPet().getId().equals(petId))
                .map(this::appointment)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(VetAppointment::eventId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public List<VetAppointment> vetAppointmentsFrom(UUID petId, LocalDate from) {
        return this.eventRepository
                .findAllByPetIdAndTypeAndRecurrenceAndLocalDateGreaterThanEqual(petId, DayEventType.VET, Recurrence.NONE, from)
                .stream()
                .map(this::appointment)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(VetAppointment::on).thenComparingInt(VetAppointment::startMinute))
                .toList();
    }

    private Optional<VetAppointment> appointment(PetDayEvent event) {
        if (event.getType() != DayEventType.VET || event.getRecurrence() != Recurrence.NONE) {
            return Optional.empty();
        }
        return this.eventService.occurrencesOn(event.getPet().getId(), event.getLocalDate()).stream()
                .filter(occurrence -> occurrence.eventId().equals(event.getId()))
                .findFirst()
                .map(occurrence -> new VetAppointment(event.getId(), event.getLocalDate(), occurrence.startMinute()));
    }
}
