package com.picvi.pet_companion_service.dayevent.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventRemoved;
import com.picvi.pet_companion_service.dayevent.domain.DayEventRules;
import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.EventDefinition;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrence;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrenceExpander;
import com.picvi.pet_companion_service.dayevent.domain.OccurrenceChange;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import com.picvi.pet_companion_service.dayevent.dto.DayEventDto;
import com.picvi.pet_companion_service.dayevent.dto.DayEventsResponse;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEvent;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEventException;
import com.picvi.pet_companion_service.dayevent.repository.PetDayEventExceptionRepository;
import com.picvi.pet_companion_service.dayevent.repository.PetDayEventRepository;
import com.picvi.pet_companion_service.exception.dayevent.DayEventNotFound;
import com.picvi.pet_companion_service.exception.dayevent.UseVetPlan;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class DayEventService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final DayEventRules eventRules;
    private final EventOccurrenceExpander occurrenceExpander;
    private final PetDayEventRepository eventRepository;
    private final PetDayEventExceptionRepository exceptionRepository;
    private final ApplicationEventPublisher events;

    public DayEventDto create(UUID ownerId, UUID petId, DayEventType type, String title, LocalDate localDate, int startMinute,
                              int durationMinutes, Recurrence recurrence, boolean planAround, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        if (type == DayEventType.VET) {
            throw new UseVetPlan();
        }
        this.eventRules.validate(startMinute, durationMinutes, title, localDate, date);

        PetDayEvent event = new PetDayEvent(this.petService.getReference(petId));
        this.apply(event, type, title, localDate, startMinute, durationMinutes, recurrence, planAround);
        return DayEventDto.of(this.eventRepository.save(event).toDefinition());
    }

    public DayEventDto update(UUID ownerId, UUID petId, UUID eventId, DayEventType type, String title, LocalDate localDate,
                              int startMinute, int durationMinutes, Recurrence recurrence, boolean planAround, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetDayEvent event = this.findEvent(petId, eventId);
        if (type == DayEventType.VET && event.getType() != DayEventType.VET) {
            throw new UseVetPlan();
        }
        this.eventRules.validate(startMinute, durationMinutes, title, localDate, date);

        this.apply(event, type, title, localDate, startMinute, durationMinutes, recurrence, planAround);
        return DayEventDto.of(this.eventRepository.save(event).toDefinition());
    }

    public void delete(UUID ownerId, UUID petId, UUID eventId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetDayEvent event = this.findEvent(petId, eventId);
        this.events.publishEvent(new DayEventRemoved(petId, eventId, event.getType()));
        this.eventRepository.delete(event);
    }

    public void changeOccurrence(UUID ownerId, UUID petId, UUID eventId, LocalDate occurrenceDate, boolean skipped,
                                 Integer startMinute, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        PetDayEvent event = this.findEvent(petId, eventId);
        if (!skipped) {
            this.eventRules.validateStart(startMinute == null ? -1 : startMinute, event.getDurationMinutes());
        }

        PetDayEventException change = this.exceptionRepository.findByEventIdAndLocalDate(eventId, occurrenceDate)
                .orElseGet(() -> new PetDayEventException(event, occurrenceDate));
        change.setSkipped(skipped);
        change.setStartMinute(skipped ? null : startMinute);
        this.exceptionRepository.save(change);
    }

    @Transactional(readOnly = true)
    public DayEventsResponse list(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        return new DayEventsResponse(this.occurrencesOn(petId, date).stream().map(occurrence -> DayEventDto.of(occurrence, date)).toList());
    }

    @Transactional(readOnly = true)
    public List<EventOccurrence> occurrencesOn(UUID petId, LocalDate date) {
        List<PetDayEvent> candidates = this.eventRepository.findPossiblyOccurringOn(petId, date);
        List<EventDefinition> definitions = candidates.stream().map(PetDayEvent::toDefinition).toList();
        List<OccurrenceChange> changes = candidates.isEmpty() ? List.of()
                : this.exceptionRepository.findAllByEventInAndLocalDate(candidates, date).stream().map(PetDayEventException::toChange).toList();
        return this.occurrenceExpander.expand(definitions, changes, date);
    }

    private PetDayEvent findEvent(UUID petId, UUID eventId) {
        return this.eventRepository.findByIdAndPetId(eventId, petId).orElseThrow(DayEventNotFound::new);
    }

    private void apply(PetDayEvent event, DayEventType type, String title, LocalDate localDate, int startMinute,
                       int durationMinutes, Recurrence recurrence, boolean planAround) {
        event.setType(type);
        event.setTitle(title);
        event.setLocalDate(localDate);
        event.setStartMinute(startMinute);
        event.setDurationMinutes(durationMinutes);
        event.setRecurrence(recurrence);
        event.setPlanAround(planAround);
    }
}
