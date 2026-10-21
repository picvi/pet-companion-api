package com.picvi.pet_companion_service.dayevent.api;

import com.picvi.pet_companion_service.dayevent.domain.DayEventRules;
import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrence;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEvent;
import com.picvi.pet_companion_service.dayevent.repository.PetDayEventRepository;
import com.picvi.pet_companion_service.dayevent.service.DayEventService;
import com.picvi.pet_companion_service.exception.dayevent.InvalidDayEvent;
import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class DayEventApiTest {
    private static final UUID PET_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate NEXT = LocalDate.of(2026, 10, 22);

    @Mock
    private PetService petService;
    @Mock
    private DayEventService eventService;
    @Mock
    private PetDayEventRepository eventRepository;

    private DayEventApi api;

    @BeforeEach
    void setUp() {
        this.api = new DayEventApi(this.petService, new DayEventRules(), this.eventService, this.eventRepository);
    }

    @Test
    void scheduleVetSavesAOneOffPlanAroundEventAndReturnsItsId() {
        Pet pet = pet();
        when(this.petService.getReference(PET_ID)).thenReturn(pet);
        when(this.eventRepository.save(any(PetDayEvent.class))).thenAnswer(call -> {
            PetDayEvent saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", EVENT_ID);
            return saved;
        });

        UUID id = this.api.scheduleVet(PET_ID, NEXT, 600, TODAY);

        ArgumentCaptor<PetDayEvent> captor = ArgumentCaptor.forClass(PetDayEvent.class);
        verify(this.eventRepository).save(captor.capture());
        PetDayEvent event = captor.getValue();
        assertThat(id).isEqualTo(EVENT_ID);
        assertThat(event.getType()).isEqualTo(DayEventType.VET);
        assertThat(event.getRecurrence()).isEqualTo(Recurrence.NONE);
        assertThat(event.getLocalDate()).isEqualTo(NEXT);
        assertThat(event.getStartMinute()).isEqualTo(600);
        assertThat(event.getDurationMinutes()).isEqualTo(DayEventApi.VET_APPOINTMENT_MINUTES);
        assertThat(event.isPlanAround()).isTrue();
        assertThat(event.getTitle()).isNull();
    }

    @Test
    void scheduleVetInThePastIsRejected() {
        assertThatThrownBy(() -> this.api.scheduleVet(PET_ID, TODAY.minusDays(1), 600, TODAY)).isInstanceOf(InvalidDayEvent.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void rescheduleVetChangesOnlyDateAndStart() {
        PetDayEvent event = vetEvent(NEXT, 600);
        event.setTitle("Dr Kowalski");
        event.setPlanAround(false);
        event.setDurationMinutes(90);
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event));

        this.api.rescheduleVet(PET_ID, EVENT_ID, NEXT.plusDays(1), 690, TODAY);

        assertThat(event.getLocalDate()).isEqualTo(NEXT.plusDays(1));
        assertThat(event.getStartMinute()).isEqualTo(690);
        assertThat(event.getTitle()).isEqualTo("Dr Kowalski");
        assertThat(event.isPlanAround()).isFalse();
        assertThat(event.getDurationMinutes()).isEqualTo(90);
        verify(this.eventRepository).save(event);
    }

    @Test
    void rescheduleVetOfAMissingEventDoesNothing() {
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.empty());

        this.api.rescheduleVet(PET_ID, EVENT_ID, NEXT, 600, TODAY);

        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void cancelDeletesTheEventAndIgnoresAMissingOne() {
        PetDayEvent event = vetEvent(NEXT, 600);
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event), Optional.empty());

        this.api.cancel(PET_ID, EVENT_ID);
        this.api.cancel(PET_ID, EVENT_ID);

        verify(this.eventRepository).delete(event);
    }

    @Test
    void vetAppointmentsFromKeepsOnlyOneOffVetEventsAndUsesTheMovedStart() {
        PetDayEvent moved = vetEvent(NEXT, 600);
        PetDayEvent skipped = vetEvent(NEXT.plusDays(1), 600, UUID.randomUUID());
        PetDayEvent grooming = vetEvent(NEXT.plusDays(2), 600, UUID.randomUUID());
        grooming.setType(DayEventType.GROOMING);
        PetDayEvent weekly = vetEvent(NEXT.plusDays(3), 600, UUID.randomUUID());
        weekly.setRecurrence(Recurrence.WEEKLY);
        when(this.eventRepository.findAllByPetIdAndTypeAndRecurrenceAndLocalDateGreaterThanEqual(PET_ID, DayEventType.VET, Recurrence.NONE, TODAY))
                .thenReturn(List.of(skipped, grooming, weekly, moved));
        when(this.eventService.occurrencesOn(PET_ID, NEXT)).thenReturn(List.of(occurrence(EVENT_ID, 690)));
        when(this.eventService.occurrencesOn(PET_ID, NEXT.plusDays(1))).thenReturn(List.of());

        List<DayEventApi.VetAppointment> appointments = this.api.vetAppointmentsFrom(PET_ID, TODAY);

        assertThat(appointments).containsExactly(new DayEventApi.VetAppointment(EVENT_ID, NEXT, 690));
    }

    @Test
    void vetAppointmentsIgnoresAnotherPetsEvents() {
        PetDayEvent own = vetEvent(NEXT, 600);
        PetDayEvent foreign = vetEvent(NEXT, 600, UUID.randomUUID());
        ReflectionTestUtils.setField(foreign, "pet", pet(UUID.randomUUID()));
        when(this.eventRepository.findAllById(List.of(EVENT_ID, foreign.getId()))).thenReturn(List.of(own, foreign));
        when(this.eventService.occurrencesOn(PET_ID, NEXT)).thenReturn(List.of(occurrence(EVENT_ID, 600)));

        var appointments = this.api.vetAppointments(PET_ID, List.of(EVENT_ID, foreign.getId()));

        assertThat(appointments).containsOnlyKeys(EVENT_ID);
    }

    private static PetDayEvent vetEvent(LocalDate on, int startMinute) {
        return vetEvent(on, startMinute, EVENT_ID);
    }

    private static PetDayEvent vetEvent(LocalDate on, int startMinute, UUID id) {
        PetDayEvent event = new PetDayEvent(pet());
        ReflectionTestUtils.setField(event, "id", id);
        event.setType(DayEventType.VET);
        event.setLocalDate(on);
        event.setStartMinute(startMinute);
        event.setDurationMinutes(60);
        event.setRecurrence(Recurrence.NONE);
        event.setPlanAround(true);
        return event;
    }

    private static EventOccurrence occurrence(UUID eventId, int startMinute) {
        return new EventOccurrence(eventId, DayEventType.VET, null, startMinute, 60, Recurrence.NONE, true);
    }

    private static Pet pet() {
        return pet(PET_ID);
    }

    private static Pet pet(UUID id) {
        Pet pet = mock(Pet.class, withSettings().strictness(Strictness.LENIENT));
        when(pet.getId()).thenReturn(id);
        return pet;
    }
}
