package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.exception.medical.VetVisitNotFound;
import com.picvi.pet_companion_service.exception.medical.VisitStatusConflict;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.medical.domain.AppointmentStatus;
import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitHistoryPaging;
import com.picvi.pet_companion_service.medical.domain.VisitOutcomeRule;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.dto.PlanVisitRequest;
import com.picvi.pet_companion_service.medical.dto.UpcomingVisitsResponse;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VisitHistoryPage;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    @Mock
    private CareApi careApi;
    @Mock
    private PetLogApi petLogApi;
    @Mock
    private PetLogRecorder petLogRecorder;
    @Mock
    private MedicationApi medicationApi;

    private VetVisitService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new VetVisitService(this.clientDateValidator, this.petService, this.repository, new VetVisitRules(),
                new VisitOutcomeRule(), new VisitHistoryPaging(), this.dayEventApi, this.careApi, this.petLogApi, this.petLogRecorder, this.medicationApi,
                new VisitLinksReader(this.careApi, this.petLogApi, this.medicationApi), new VetAppointmentRule(), clock);
        lenient().when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(
                new PetProfile(PET, "Mila", BORN, null, TODAY, null, null, 0));
    }

    @Test
    void planningSavesAPlannedVisitAndBooksItInHerDay() {
        when(this.repository.findByPetIdAndClientId(eq(PET), any())).thenReturn(Optional.empty());
        when(this.dayEventApi.scheduleVet(PET, NEXT, MINUTE, TODAY)).thenReturn(EVENT);
        when(this.repository.saveAndFlush(any(VetVisit.class))).thenAnswer(call -> call.getArgument(0));

        VetVisitService.CreateResult result = this.service.plan(OWNER, PET, plan(UUID.randomUUID()));

        assertThat(result.created()).isTrue();
        VetVisitDto dto = result.visit();
        assertThat(dto.status()).isEqualTo(VisitStatus.PLANNED);
        assertThat(dto.visitedOn()).isEqualTo(NEXT);
        assertThat(dto.scheduledMinute()).isEqualTo(MINUTE);
        assertThat(dto.clinic()).isEqualTo("VetCare");
        assertThat(dto.prepNote()).isEqualTo("Bring the vaccine booklet");
    }

    @Test
    void aRetriedPlanReturnsTheSavedVisitWithoutBookingAgain() {
        UUID clientId = UUID.randomUUID();
        VetVisit saved = planned(clientId);
        when(this.repository.findByPetIdAndClientId(PET, clientId)).thenReturn(Optional.of(saved));

        VetVisitService.CreateResult result = this.service.plan(OWNER, PET, plan(clientId));

        assertThat(result.created()).isFalse();
        verify(this.dayEventApi, never()).scheduleVet(any(), any(), anyInt(), any());
        verify(this.repository, never()).saveAndFlush(any());
    }

    @Test
    void aPlanInThePastOrTooFarAheadIsRejected() {
        when(this.repository.findByPetIdAndClientId(eq(PET), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.plan(OWNER, PET, plan(UUID.randomUUID(), TODAY.minusDays(1), MINUTE, null, null)))
                .hasMessage("scheduledOn: OUT_OF_RANGE");
        assertThatThrownBy(() -> this.service.plan(OWNER, PET, plan(UUID.randomUUID(), TODAY.plusYears(3), MINUTE, null, null)))
                .hasMessage("scheduledOn: OUT_OF_RANGE");
        verify(this.dayEventApi, never()).scheduleVet(any(), any(), anyInt(), any());
    }

    @Test
    void aTooLongClinicOrPrepNoteIsRejected() {
        when(this.repository.findByPetIdAndClientId(eq(PET), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.plan(OWNER, PET, plan(UUID.randomUUID(), NEXT, MINUTE, "x".repeat(121), null)))
                .hasMessage("clinic: TOO_LONG");
        assertThatThrownBy(() -> this.service.plan(OWNER, PET, plan(UUID.randomUUID(), NEXT, MINUTE, null, "x".repeat(301))))
                .hasMessage("prepNote: TOO_LONG");
    }

    @Test
    void aPlanWithoutAClientIdIsRejected() {
        assertThatThrownBy(() -> this.service.plan(OWNER, PET, plan(null)))
                .isInstanceOf(InvalidVetVisit.class)
                .hasMessage("clientId: REQUIRED");
    }

    @Test
    void changingAPlanMovesItsEventInHerDay() {
        VetVisit visit = plannedWithEvent();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));
        when(this.repository.saveAndFlush(visit)).thenReturn(visit);

        VetVisitDto dto = this.service.updatePlan(OWNER, PET, id, visit.getVersion(),
                plan(null, NEXT.plusDays(2), 720, "Other clinic", null));

        assertThat(dto.visitedOn()).isEqualTo(NEXT.plusDays(2));
        assertThat(dto.clinic()).isEqualTo("Other clinic");
        verify(this.dayEventApi).rescheduleVet(PET, EVENT, NEXT.plusDays(2), 720, TODAY);
    }

    @Test
    void changingAPlanWithAStaleVersionConflicts() {
        VetVisit visit = plannedWithEvent();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> this.service.updatePlan(OWNER, PET, id, 5, plan(null)))
                .isInstanceOf(VersionConflict.class);
    }

    @Test
    void aVisitThatAlreadyHappenedCannotBeReplanned() {
        VetVisit done = visit(UUID.randomUUID());
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(done));

        assertThatThrownBy(() -> this.service.updatePlan(OWNER, PET, id, done.getVersion(), plan(null)))
                .isInstanceOf(VisitStatusConflict.class);
        verify(this.dayEventApi, never()).rescheduleVet(any(), any(), any(), anyInt(), any());
    }

    @Test
    void getOfAnotherPetsVisitIsNotFound() {
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.get(OWNER, PET, id)).isInstanceOf(VetVisitNotFound.class);
    }

    @Test
    void deletingAPlanCancelsItsEventInHerDay() {
        VetVisit visit = plannedWithEvent();
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.of(visit));

        this.service.delete(OWNER, PET, id, false, null, null);

        verify(this.dayEventApi).cancel(PET, EVENT);
        verify(this.repository).delete(visit);
    }

    @Test
    void deletingAVisitThatIsGoneIsFine() {
        UUID id = UUID.randomUUID();
        when(this.repository.findByIdAndPetId(id, PET)).thenReturn(Optional.empty());

        this.service.delete(OWNER, PET, id, false, null, null);

        verify(this.repository, never()).delete(any(VetVisit.class));
    }

    @Test
    void upcomingListsThePlansTheLastVisitAndTheNextAppointment() {
        VetVisit plan = plannedWithEvent();
        VetVisit last = visit(UUID.randomUUID());
        when(this.repository.findAllByPetIdAndStatusAndVisitedOnGreaterThanEqualOrderByVisitedOnAscScheduledMinuteAsc(
                PET, VisitStatus.PLANNED, TODAY.minusDays(3))).thenReturn(List.of(plan));
        when(this.dayEventApi.vetAppointments(PET, List.of(EVENT))).thenReturn(Map.of(EVENT, new DayEventApi.VetAppointment(EVENT, NEXT, 690)));
        when(this.repository.findFirstByPetIdAndStatusAndVisitedOnLessThanEqualOrderByVisitedOnDescCreatedAtDesc(PET, VisitStatus.DONE, TODAY))
                .thenReturn(Optional.of(last));

        UpcomingVisitsResponse response = this.service.upcoming(OWNER, PET, TODAY);

        assertThat(response.plans()).hasSize(1);
        assertThat(response.plans().get(0).appointment().startMinute()).isEqualTo(690);
        assertThat(response.lastVisit().visitedOn()).isEqualTo(TODAY);
        assertThat(response.nextVetAppointment().startMinute()).isEqualTo(690);
        assertThat(response.nextVetAppointment().status()).isEqualTo(AppointmentStatus.UPCOMING);
    }

    @Test
    void upcomingWithNothingPlannedHasNoAppointment() {
        when(this.repository.findAllByPetIdAndStatusAndVisitedOnGreaterThanEqualOrderByVisitedOnAscScheduledMinuteAsc(
                PET, VisitStatus.PLANNED, TODAY.minusDays(3))).thenReturn(List.of());
        when(this.dayEventApi.vetAppointments(PET, List.of())).thenReturn(Map.of());
        when(this.repository.findFirstByPetIdAndStatusAndVisitedOnLessThanEqualOrderByVisitedOnDescCreatedAtDesc(PET, VisitStatus.DONE, TODAY))
                .thenReturn(Optional.empty());

        UpcomingVisitsResponse response = this.service.upcoming(OWNER, PET, TODAY);

        assertThat(response.plans()).isEmpty();
        assertThat(response.lastVisit()).isNull();
        assertThat(response.nextVetAppointment()).isNull();
    }

    @Test
    void historyReadsLinksOncePerModuleAndMarksTheNextPage() {
        List<VetVisit> rows = List.of(doneOn(TODAY), doneOn(TODAY.minusDays(1)), doneOn(TODAY.minusDays(2)));
        when(this.repository.findAllByPetIdAndStatusAndVisitedOnLessThanOrderByVisitedOnDescCreatedAtDesc(
                eq(PET), eq(VisitStatus.DONE), any(), any())).thenReturn(rows);

        VisitHistoryPage page = this.service.history(OWNER, PET, null, 2);

        assertThat(page.visits()).extracting(VetVisitDto::visitedOn).containsExactly(TODAY, TODAY.minusDays(1));
        assertThat(page.nextBefore()).isEqualTo(TODAY.minusDays(1));
        verify(this.careApi).events(eq(PET), any());
        verify(this.petLogApi).snapshots(eq(PET), any());
        verify(this.medicationApi).courses(eq(PET), any(), any());
    }

    @Test
    void historyOfAnotherPetsVisitsIsRefused() {
        when(this.petService.getOwnedPetProfile(OWNER, PET)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.history(OWNER, PET, null, null)).isInstanceOf(PetNotFound.class);
    }

    private static VetVisit doneOn(LocalDate day) {
        VetVisit visit = VetVisit.done(PET, UUID.randomUUID(), day, "VetCare", Instant.parse("2026-10-15T10:00:00Z"));
        ReflectionTestUtils.setField(visit, "id", UUID.randomUUID());
        return visit;
    }

    private static PlanVisitRequest plan(UUID clientId) {
        return plan(clientId, NEXT, MINUTE, "VetCare", "Bring the vaccine booklet");
    }

    private static PlanVisitRequest plan(UUID clientId, LocalDate on, int minute, String clinic, String prepNote) {
        return new PlanVisitRequest(clientId, TODAY, on, minute, clinic, prepNote);
    }

    private static VetVisit planned(UUID clientId) {
        return VetVisit.planned(PET, clientId, NEXT, MINUTE, "VetCare", null, Instant.parse("2026-10-15T10:00:00Z"));
    }

    private static VetVisit plannedWithEvent() {
        VetVisit visit = planned(UUID.randomUUID());
        visit.setDayEventId(EVENT);
        return visit;
    }

    private static VetVisit visit(UUID clientId) {
        return VetVisit.done(PET, clientId, TODAY, "VetCare", Instant.parse("2026-10-15T10:00:00Z"));
    }
}
