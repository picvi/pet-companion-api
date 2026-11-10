package com.picvi.pet_companion_service.dayevent.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventRemoved;
import com.picvi.pet_companion_service.dayevent.domain.DayEventRules;
import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrence;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrenceExpander;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import com.picvi.pet_companion_service.dayevent.dto.DayEventDto;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEvent;
import com.picvi.pet_companion_service.dayevent.entity.PetDayEventException;
import com.picvi.pet_companion_service.dayevent.repository.PetDayEventExceptionRepository;
import com.picvi.pet_companion_service.dayevent.repository.PetDayEventRepository;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.dayevent.DayEventNotFound;
import com.picvi.pet_companion_service.exception.dayevent.InvalidDayEvent;
import com.picvi.pet_companion_service.exception.dayevent.UseVetPlan;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DayEventServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final PetProfile PET = new PetProfile(PET_ID, "Mila", TODAY.minusWeeks(11), SizeCategory.SMALL,
            TODAY.minusWeeks(11), VaccinationStatus.UNKNOWN, null, 0);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private PetDayEventRepository eventRepository;
    @Mock
    private PetDayEventExceptionRepository exceptionRepository;
    @Mock
    private ApplicationEventPublisher events;

    private DayEventService service;

    @BeforeEach
    void setUp() {
        this.service = new DayEventService(this.clientDateValidator, this.petService, new DayEventRules(),
                new EventOccurrenceExpander(), this.eventRepository, this.exceptionRepository, this.events);
    }

    @Test
    void createsAnEvent() {
        this.givenOwnedPet();
        when(this.eventRepository.save(any(PetDayEvent.class))).thenAnswer(call -> call.getArgument(0));

        DayEventDto created = this.service.create(OWNER_ID, PET_ID, DayEventType.GROOMING, "Nails", TODAY, 600, 60,
                Recurrence.NONE, true, TODAY);

        ArgumentCaptor<PetDayEvent> saved = ArgumentCaptor.forClass(PetDayEvent.class);
        verify(this.eventRepository).save(saved.capture());
        assertThat(saved.getValue().getStartMinute()).isEqualTo(600);
        assertThat(saved.getValue().isPlanAround()).isTrue();
        assertThat(created.type()).isEqualTo(DayEventType.GROOMING);
        assertThat(created.title()).isEqualTo("Nails");
    }

    @Test
    void doesNotCreateAnInvalidEvent() {
        this.givenOwnedPet();

        assertThatThrownBy(() -> this.service.create(OWNER_ID, PET_ID, DayEventType.GROOMING, null, TODAY.minusDays(1), 600, 60,
                Recurrence.NONE, true, TODAY)).isInstanceOf(InvalidDayEvent.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void rejectsADateTheClientValidatorRefuses() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.create(OWNER_ID, PET_ID, DayEventType.GROOMING, null, TODAY, 600, 60,
                Recurrence.NONE, true, TODAY)).isInstanceOf(InvalidDate.class);
    }

    @Test
    void updatesAnExistingEvent() {
        this.givenOwnedPet();
        PetDayEvent event = this.existingEvent();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event));
        when(this.eventRepository.save(event)).thenReturn(event);

        DayEventDto updated = this.service.update(OWNER_ID, PET_ID, EVENT_ID, DayEventType.GROOMING, null, TODAY.plusDays(2), 900,
                90, Recurrence.WEEKLY, false, TODAY);

        assertThat(updated.type()).isEqualTo(DayEventType.GROOMING);
        assertThat(event.getStartMinute()).isEqualTo(900);
        assertThat(event.getRecurrence()).isEqualTo(Recurrence.WEEKLY);
    }

    @Test
    void aVetEventIsBookedByPlanningAVisitNotCreatedHere() {
        this.givenOwnedPet();

        assertThatThrownBy(() -> this.service.create(OWNER_ID, PET_ID, DayEventType.VET, null, TODAY, 600, 60,
                Recurrence.NONE, true, TODAY)).isInstanceOf(UseVetPlan.class);
        verify(this.eventRepository, never()).save(any());
    }

    @Test
    void anEventCannotBecomeAVetEvent() {
        this.givenOwnedPet();
        PetDayEvent event = this.existingEvent();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, EVENT_ID, DayEventType.VET, null, TODAY, 600, 60,
                Recurrence.NONE, true, TODAY)).isInstanceOf(UseVetPlan.class);
    }

    @Test
    void deletingAnEventTellsTheOthersFirst() {
        this.givenOwnedPet();
        PetDayEvent event = this.existingEvent();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event));

        this.service.delete(OWNER_ID, PET_ID, EVENT_ID, TODAY);

        InOrder order = inOrder(this.events, this.eventRepository);
        order.verify(this.events).publishEvent(new DayEventRemoved(PET_ID, EVENT_ID, event.getType()));
        order.verify(this.eventRepository).delete(event);
    }

    @Test
    void failsToUpdateAnUnknownEvent() {
        this.givenOwnedPet();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, EVENT_ID, DayEventType.GROOMING, null, TODAY, 600, 60,
                Recurrence.NONE, true, TODAY)).isInstanceOf(DayEventNotFound.class);
    }

    @Test
    void deletesAnEvent() {
        this.givenOwnedPet();
        PetDayEvent event = this.existingEvent();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event));

        this.service.delete(OWNER_ID, PET_ID, EVENT_ID, TODAY);

        verify(this.eventRepository).delete(event);
    }

    @Test
    void skippingOneWeekKeepsTheStartOutOfTheChange() {
        this.givenOwnedPet();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(this.existingEvent()));
        when(this.exceptionRepository.findByEventIdAndLocalDate(EVENT_ID, TODAY.plusWeeks(1))).thenReturn(Optional.empty());

        this.service.changeOccurrence(OWNER_ID, PET_ID, EVENT_ID, TODAY.plusWeeks(1), true, 700, TODAY);

        ArgumentCaptor<PetDayEventException> saved = ArgumentCaptor.forClass(PetDayEventException.class);
        verify(this.exceptionRepository).save(saved.capture());
        assertThat(saved.getValue().isSkipped()).isTrue();
        assertThat(saved.getValue().getStartMinute()).isNull();
    }

    @Test
    void movingOneWeekReusesTheExistingChange() {
        this.givenOwnedPet();
        PetDayEvent event = this.existingEvent();
        PetDayEventException existing = new PetDayEventException(event, TODAY.plusWeeks(1));
        existing.setSkipped(true);
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(event));
        when(this.exceptionRepository.findByEventIdAndLocalDate(EVENT_ID, TODAY.plusWeeks(1))).thenReturn(Optional.of(existing));

        this.service.changeOccurrence(OWNER_ID, PET_ID, EVENT_ID, TODAY.plusWeeks(1), false, 700, TODAY);

        verify(this.exceptionRepository).save(existing);
        assertThat(existing.isSkipped()).isFalse();
        assertThat(existing.getStartMinute()).isEqualTo(700);
    }

    @Test
    void movingWithoutANewStartIsRejected() {
        this.givenOwnedPet();
        when(this.eventRepository.findByIdAndPetId(EVENT_ID, PET_ID)).thenReturn(Optional.of(this.existingEvent()));

        assertThatThrownBy(() -> this.service.changeOccurrence(OWNER_ID, PET_ID, EVENT_ID, TODAY.plusWeeks(1), false, null, TODAY))
                .isInstanceOf(InvalidDayEvent.class);
    }

    @Test
    void listsTheOccurrencesOfADayWithTheirChanges() {
        PetDayEvent weekly = this.existingEvent();
        weekly.setRecurrence(Recurrence.WEEKLY);
        when(this.eventRepository.findPossiblyOccurringOn(PET_ID, TODAY.plusWeeks(1))).thenReturn(List.of(weekly));
        PetDayEventException move = new PetDayEventException(weekly, TODAY.plusWeeks(1));
        move.setStartMinute(720);
        when(this.exceptionRepository.findAllByEventInAndLocalDate(List.of(weekly), TODAY.plusWeeks(1))).thenReturn(List.of(move));

        List<EventOccurrence> occurrences = this.service.occurrencesOn(PET_ID, TODAY.plusWeeks(1));

        assertThat(occurrences).extracting(EventOccurrence::startMinute).containsExactly(720);
    }

    private void givenOwnedPet() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(PET);
    }

    private PetDayEvent existingEvent() {
        PetDayEvent event = new PetDayEvent(null);
        ReflectionTestUtils.setField(event, "id", EVENT_ID);
        event.setType(DayEventType.CLASS);
        event.setLocalDate(TODAY);
        event.setStartMinute(600);
        event.setDurationMinutes(60);
        event.setRecurrence(Recurrence.NONE);
        event.setPlanAround(true);
        return event;
    }
}
