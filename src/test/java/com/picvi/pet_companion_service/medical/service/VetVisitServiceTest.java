package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.exception.medical.VetVisitNotFound;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medical.domain.AppointmentStatus;
import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitReason;
import com.picvi.pet_companion_service.medical.dto.CreateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.UpdateVetVisitRequest;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VetVisitListResponse;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetVisitServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate BORN = LocalDate.of(2026, 7, 30);
    private static final LocalDate NEXT = LocalDate.of(2026, 10, 22);
    private static final int MINUTE = 600;
    private static final UUID EVENT = UUID.randomUUID();

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private VetVisitRepository repository;
    @Mock
    private DayEventApi dayEventApi;

    private VetVisitService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new VetVisitService(this.clientDateValidator, this.petService, this.repository, new VetVisitRules(),
                this.dayEventApi, new VetAppointmentRule(), clock);
        lenient().when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(
                new PetProfile(PET, "Mila", BORN, null, TODAY, null, null, 0));
    }

    @Test
    void createSavesANewVisitAndReportsCreated() {
        UUID clientId = UUID.randomUUID();
        when(this.repository.findByPetIdAndClientId(PET, clientId)).thenReturn(Optional.empty());
        when(this.repository.saveAndFlush(any(VetVisit.class))).thenAnswer(call -> call.getArgument(0));

        VetVisitService.CreateResult result = this.service.create(OWNER, PET, create(clientId, TODAY, null, null, List.of(VisitOutcome.TREATMENT, VisitOutcome.TESTS)));

        assertThat(result.created()).isTrue();
        assertThat(result.visit().outcomes()).containsExactly(VisitOutcome.TREATMENT, VisitOutcome.TESTS);
    }

    @Test
    void createWithTheSameClientIdAgainReturnsTheExistingVisit() {
        UUID clientId = UUID.randomUUID();
        VetVisit existing = visit(clientId);
        when(this.repository.findByPetIdAndClientId(PET, clientId)).thenReturn(Optional.of(existing));

        VetVisitService.CreateResult result = this.service.create(OWNER, PET, create(clientId, TODAY, null, null, List.of()));

        assertThat(result.created()).isFalse();
        verify(this.repository, never()).saveAndFlush(any());
    }

    @Test
    void nextCheckOnNotAfterTheVisitIsRejected() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, TODAY, MINUTE, List.of())))
                .isInstanceOf(InvalidVetVisit.class)
                .hasMessage("nextCheckOn: OUT_OF_RANGE");
    }

    @Test
    void fineCannotCombineWithOtherOutcomes() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, null, null, List.of(VisitOutcome.FINE, VisitOutcome.TREATMENT))))
                .isInstanceOf(InvalidVetVisit.class)
                .hasMessage("outcomes: INVALID");
    }

    @Test
    void duplicateOutcomesAreRejected() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, null, null, List.of(VisitOutcome.TESTS, VisitOutcome.TESTS))))
                .isInstanceOf(InvalidVetVisit.class)
                .hasMessage("outcomes: INVALID");
    }

    @Test
    void aVisitBeforeBirthOrInTheFutureIsRejected() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), BORN.minusDays(1), null, null, List.of())))
                .hasMessage("visitedOn: OUT_OF_RANGE");
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY.plusDays(2), null, null, List.of())))
                .hasMessage("visitedOn: OUT_OF_RANGE");
    }

    @Test
    void tooLongNoteAndClinicAreRejected() {
        CreateVetVisitRequest longNote = new CreateVetVisitRequest(UUID.randomUUID(), TODAY, TODAY, VisitReason.CHECK_UP, null, null, "x".repeat(501), null, null, null);
        CreateVetVisitRequest longClinic = new CreateVetVisitRequest(UUID.randomUUID(), TODAY, TODAY, VisitReason.CHECK_UP, null, "x".repeat(121), null, null, null, null);

        assertThatThrownBy(() -> this.service.create(OWNER, PET, longNote)).hasMessage("note: TOO_LONG");
        assertThatThrownBy(() -> this.service.create(OWNER, PET, longClinic)).hasMessage("clinic: TOO_LONG");
    }

    @Test
    void updateWithAStaleVersionConflicts() {
        VetVisit visit = visit(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> this.service.update(OWNER, PET, id, 5, update()))
                .isInstanceOf(VersionConflict.class);
    }

    @Test
    void updateReplacesTheEditableFields() {
        VetVisit visit = visit(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);

        var dto = this.service.update(OWNER, PET, id, visit.getVersion(), update());

        assertThat(dto.clinic()).isEqualTo("VetCare");
        assertThat(dto.reason()).isEqualTo(VisitReason.VACCINE_OR_TREATMENT);
    }

    @Test
    void getOfAnotherPetsVisitIsNotFound() {
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.get(OWNER, PET, id)).isInstanceOf(VetVisitNotFound.class);
    }

    @Test
    void deleteIsIdempotent() {
        UUID id = UUID.randomUUID();
        VetVisit visit = visit(UUID.randomUUID());
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit), Optional.empty());

        this.service.delete(OWNER, PET, id);
        this.service.delete(OWNER, PET, id);

        verify(this.repository).delete(visit);
    }

    @Test
    void createWithANextCheckSchedulesItInHerDay() {
        UUID clientId = UUID.randomUUID();
        when(this.repository.findByPetIdAndClientId(PET, clientId)).thenReturn(Optional.empty());
        when(this.repository.saveAndFlush(any(VetVisit.class))).thenAnswer(call -> call.getArgument(0));
        when(this.dayEventApi.scheduleVet(PET, NEXT, MINUTE, TODAY)).thenReturn(EVENT);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(NEXT, MINUTE)));

        VetVisitDto dto = this.service.create(OWNER, PET, create(clientId, TODAY, NEXT, MINUTE, List.of())).visit();

        assertThat(dto.nextCheckOn()).isEqualTo(NEXT);
        assertThat(dto.nextCheckMinute()).isEqualTo(MINUTE);
    }

    @Test
    void createWithoutANextCheckDoesNotTouchHerDay() {
        when(this.repository.saveAndFlush(any(VetVisit.class))).thenAnswer(call -> call.getArgument(0));

        VetVisitDto dto = this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, null, null, List.of())).visit();

        assertThat(dto.nextCheckOn()).isNull();
        assertThat(dto.nextCheckMinute()).isNull();
        verify(this.dayEventApi, never()).scheduleVet(any(), any(), anyInt(), any());
    }

    @Test
    void aNextCheckNeedsATime() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, NEXT, null, List.of())))
                .hasMessage("nextCheckMinute: REQUIRED");
    }

    @Test
    void aNextCheckTimeNeedsADate() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, null, MINUTE, List.of())))
                .hasMessage("nextCheckOn: REQUIRED");
    }

    @Test
    void aNextCheckTimeThatDoesNotFitTheDayIsRejected() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY, NEXT, 1400, List.of())))
                .hasMessage("nextCheckMinute: OUT_OF_RANGE");
    }

    @Test
    void aNextCheckInThePastIsRejected() {
        assertThatThrownBy(() -> this.service.create(OWNER, PET, create(UUID.randomUUID(), TODAY.minusDays(3), TODAY.minusDays(1), MINUTE, List.of())))
                .hasMessage("nextCheckOn: OUT_OF_RANGE");
    }

    @Test
    void updateWithoutANextCheckCancelsTheLinkedEvent() {
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(NEXT, MINUTE)));

        this.service.update(OWNER, PET, id, visit.getVersion(), update());

        verify(this.dayEventApi).cancel(PET, EVENT);
        assertThat(visit.getNextCheckEventId()).isNull();
    }

    @Test
    void updateWithoutANextCheckWhenTheEventIsGoneOnlyClearsTheLink() {
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of());

        this.service.update(OWNER, PET, id, visit.getVersion(), update());

        assertThat(visit.getNextCheckEventId()).isNull();
    }

    @Test
    void updateWithAnUnchangedNextCheckLeavesHerDayAlone() {
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(NEXT, MINUTE)));

        this.service.update(OWNER, PET, id, visit.getVersion(), update(NEXT, MINUTE));

        verify(this.dayEventApi, never()).rescheduleVet(any(), any(), any(), anyInt(), any());
        verify(this.dayEventApi, never()).scheduleVet(any(), any(), anyInt(), any());
        verify(this.dayEventApi, never()).cancel(any(), any());
    }

    @Test
    void updateWithAPastAppointmentSentUnchangedDoesNotFail() {
        LocalDate past = TODAY.minusDays(2);
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(past, MINUTE)));

        VetVisitDto dto = this.service.update(OWNER, PET, id, visit.getVersion(), update(past, MINUTE));

        assertThat(dto.nextCheckOn()).isEqualTo(past);
    }

    @Test
    void updateWithAChangedNextCheckMovesTheLinkedEvent() {
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(NEXT, MINUTE)));

        this.service.update(OWNER, PET, id, visit.getVersion(), update(NEXT.plusDays(1), 690));

        verify(this.dayEventApi).rescheduleVet(PET, EVENT, NEXT.plusDays(1), 690, TODAY);
    }

    @Test
    void updateWithANextCheckAndNoLinkedEventSchedulesOne() {
        VetVisit visit = visit(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.scheduleVet(PET, NEXT, MINUTE, TODAY)).thenReturn(EVENT);
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(NEXT, MINUTE)));

        VetVisitDto dto = this.service.update(OWNER, PET, id, visit.getVersion(), update(NEXT, MINUTE));

        assertThat(visit.getNextCheckEventId()).isEqualTo(EVENT);
        assertThat(dto.nextCheckMinute()).isEqualTo(MINUTE);
    }

    @Test
    void updateWhenTheEventWasRemovedInHerDaySchedulesANewOne() {
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        UUID newEvent = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);
        when(this.dayEventApi.vetAppointments(eq(PET), any())).thenReturn(Map.of());
        when(this.dayEventApi.scheduleVet(PET, NEXT, MINUTE, TODAY)).thenReturn(newEvent);

        this.service.update(OWNER, PET, id, visit.getVersion(), update(NEXT, MINUTE));

        assertThat(visit.getNextCheckEventId()).isEqualTo(newEvent);
    }

    @Test
    void deleteCancelsTheLinkedEvent() {
        VetVisit visit = visitWithAppointment();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));

        this.service.delete(OWNER, PET, id);

        verify(this.dayEventApi).cancel(PET, EVENT);
        verify(this.repository).delete(visit);
    }

    @Test
    void listReturnsTheNextVetAppointmentWhoeverBookedIt() {
        VetVisit booking = visitWithAppointment();
        UUID bookingId = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(booking, "id", bookingId);
        UUID fromDay = UUID.randomUUID();
        when(this.repository.findAllByPetIdOrderByVisitedOnDescCreatedAtDesc(PET)).thenReturn(List.of(booking));
        when(this.dayEventApi.vetAppointmentsFrom(PET, TODAY.minusDays(3)))
                .thenReturn(List.of(appointment(NEXT, MINUTE), new DayEventApi.VetAppointment(fromDay, NEXT.plusDays(5), 540)));
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, appointment(NEXT, MINUTE)));

        VetVisitListResponse response = this.service.list(OWNER, PET, TODAY);

        assertThat(response.visits()).hasSize(1);
        assertThat(response.visits().getFirst().nextCheckOn()).isEqualTo(NEXT);
        assertThat(response.nextVetAppointment().eventId()).isEqualTo(EVENT);
        assertThat(response.nextVetAppointment().visitId()).isEqualTo(bookingId);
        assertThat(response.nextVetAppointment().status()).isEqualTo(AppointmentStatus.UPCOMING);
    }

    @Test
    void listWithNothingScheduledHasNoNextVetAppointment() {
        when(this.repository.findAllByPetIdOrderByVisitedOnDescCreatedAtDesc(PET)).thenReturn(List.of(visit(UUID.randomUUID())));
        when(this.dayEventApi.vetAppointmentsFrom(PET, TODAY.minusDays(3))).thenReturn(List.of());
        when(this.dayEventApi.vetAppointments(eq(PET), any())).thenReturn(Map.of());

        VetVisitListResponse response = this.service.list(OWNER, PET, TODAY);

        assertThat(response.nextVetAppointment()).isNull();
        assertThat(response.visits().getFirst().nextCheckOn()).isNull();
    }

    @Test
    void listCountsAnAppointmentAddedInHerDayWithNoVisit() {
        when(this.repository.findAllByPetIdOrderByVisitedOnDescCreatedAtDesc(PET)).thenReturn(List.of());
        when(this.dayEventApi.vetAppointmentsFrom(PET, TODAY.minusDays(3))).thenReturn(List.of(appointment(TODAY.plusDays(1), 540)));
        when(this.dayEventApi.vetAppointments(eq(PET), any())).thenReturn(Map.of());

        VetVisitListResponse response = this.service.list(OWNER, PET, TODAY);

        assertThat(response.nextVetAppointment().visitId()).isNull();
        assertThat(response.nextVetAppointment().startMinute()).isEqualTo(540);
    }

    private static CreateVetVisitRequest create(UUID clientId, LocalDate visitedOn, LocalDate nextCheckOn, Integer nextCheckMinute,
                                                List<VisitOutcome> outcomes) {
        return new CreateVetVisitRequest(clientId, TODAY, visitedOn, VisitReason.SOMETHING_WRONG, outcomes, "VetCare", null,
                nextCheckOn, nextCheckMinute, null);
    }

    private static UpdateVetVisitRequest update() {
        return update(null, null);
    }

    private static UpdateVetVisitRequest update(LocalDate nextCheckOn, Integer nextCheckMinute) {
        return new UpdateVetVisitRequest(TODAY, TODAY, VisitReason.VACCINE_OR_TREATMENT, List.of(), "VetCare", null, nextCheckOn,
                nextCheckMinute, null);
    }

    private static DayEventApi.VetAppointment appointment(LocalDate on, int startMinute) {
        return new DayEventApi.VetAppointment(EVENT, on, startMinute);
    }

    private VetVisit visitWithAppointment() {
        VetVisit visit = visit(UUID.randomUUID());
        visit.setNextCheckEventId(EVENT);
        return visit;
    }

    private static VetVisit visit(UUID clientId) {
        return new VetVisit(PET, clientId, TODAY, VisitReason.CHECK_UP, EnumSet.noneOf(VisitOutcome.class), null, null, null,
                TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
