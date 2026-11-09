package com.picvi.pet_companion_service.medical.service;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.exception.medical.VisitStatusConflict;
import com.picvi.pet_companion_service.exception.medication.InvalidCourse;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medical.domain.VetAppointmentRule;
import com.picvi.pet_companion_service.medical.domain.VetVisitRules;
import com.picvi.pet_companion_service.medical.domain.VisitChildIds;
import com.picvi.pet_companion_service.medical.domain.VisitLogRole;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitOutcomeRule;
import com.picvi.pet_companion_service.medical.domain.VisitProcedureKind;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.domain.VisitTestKind;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.ClinicMedicine;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.Procedure;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.TestRequest;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.repository.VetVisitRepository;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.PetLogRecorder;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.picvi.pet_companion_service.medical.LogVisitRequests.TODAY;
import static com.picvi.pet_companion_service.medical.LogVisitRequests.aCourse;
import static com.picvi.pet_companion_service.medical.LogVisitRequests.aVisit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetVisitLogServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final UUID PLAN_EVENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

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
        this.service = new VetVisitService(this.clientDateValidator, this.petService, this.repository, new VetVisitRules(),
                new VisitOutcomeRule(), this.dayEventApi, this.careApi, this.petLogApi, this.petLogRecorder, this.medicationApi,
                new VisitLinksReader(this.careApi, this.petLogApi, this.medicationApi), new VetAppointmentRule(),
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(
                new PetProfile(PET, "Mila", LocalDate.of(2026, 7, 30), null, TODAY, null, null, 0));
        lenient().when(this.repository.save(any(VetVisit.class))).thenAnswer(call -> withId(call.getArgument(0)));
        lenient().when(this.repository.saveAndFlush(any(VetVisit.class))).thenAnswer(call -> withId(call.getArgument(0)));
    }

    @Test
    void aNewVisitLinksWhatHappenedAndDerivesItsOutcomes() {
        LogVisitRequest request = aVisit()
                .careItemCodes("DHPPI")
                .clinicMedicines(new ClinicMedicine("Convenia", "0.5 ml"))
                .homeCourses(aCourse("Metronidazole"))
                .weightGrams(4200)
                .tests(new TestRequest(VisitTestKind.BLOOD, true))
                .followUp(TODAY.plusDays(7), 600)
                .build();
        UUID care = UUID.randomUUID();
        UUID medicineLog = UUID.randomUUID();
        UUID weightLog = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        when(this.repository.findByPetIdAndClientId(PET, request.clientId())).thenReturn(Optional.empty());
        when(this.careApi.log(OWNER, PET, "DHPPI", TODAY, TODAY)).thenReturn(new CareEventDto(care, "DHPPI", TODAY));
        when(this.petLogRecorder.logMedicine(OWNER, PET, VisitChildIds.of(request.clientId(), "medicine-0", 0), "Convenia", "0.5 ml", TODAY, 600, TODAY))
                .thenReturn(medicineLog);
        when(this.petLogRecorder.logWeight(OWNER, PET, VisitChildIds.of(request.clientId(), "weight-0", 0), 4200, TODAY, 600, TODAY))
                .thenReturn(weightLog);
        when(this.medicationApi.start(eq(OWNER), eq(PET), any())).thenReturn(course);
        when(this.dayEventApi.scheduleVet(PET, TODAY.plusDays(7), 600, TODAY)).thenReturn(PLAN_EVENT);

        VetVisitService.CreateResult result = this.service.log(OWNER, PET, request);

        assertThat(result.created()).isTrue();
        VetVisitDto dto = result.visit();
        assertThat(dto.status()).isEqualTo(VisitStatus.DONE);
        assertThat(dto.outcomes()).containsExactly(VisitOutcome.TREATMENT, VisitOutcome.TESTS, VisitOutcome.BACK);
        assertThat(dto.tests()).containsExactly(new VetVisitDto.TestDto(VisitTestKind.BLOOD, true));
        verify(this.repository).save(argThat(plan -> plan.isPlanned() && plan.getDayEventId() == PLAN_EVENT
                && plan.getClinic().equals("VetCare") && plan.getVisitedOn().equals(TODAY.plusDays(7))));
    }

    @Test
    void aRetriedLogReturnsTheSavedVisitAndWritesNothing() {
        LogVisitRequest request = aVisit().careItemCodes("DHPPI").clinicMedicines(new ClinicMedicine("Convenia", null)).build();
        VetVisit saved = done(request.clientId());
        when(this.repository.findByPetIdAndClientId(PET, request.clientId())).thenReturn(Optional.of(saved));

        VetVisitService.CreateResult result = this.service.log(OWNER, PET, request);

        assertThat(result.created()).isFalse();
        verifyNoInteractions(this.petLogRecorder, this.medicationApi);
        verify(this.careApi, never()).log(any(), any(), any(), any(), any());
    }

    @Test
    void aCareEventAlreadyLoggedForThatDayIsLinkedNotLoggedAgain() {
        LogVisitRequest request = aVisit().careItemCodes("DHPPI").build();
        UUID existing = UUID.randomUUID();
        when(this.repository.findByPetIdAndClientId(PET, request.clientId())).thenReturn(Optional.empty());
        when(this.careApi.findEvent(PET, "DHPPI", TODAY)).thenReturn(Optional.of(new CareEventDto(existing, "DHPPI", TODAY)));

        this.service.log(OWNER, PET, request);

        verify(this.careApi, never()).log(any(), any(), any(), any(), any());
        verify(this.repository).saveAndFlush(argThat(visit -> visit.getCareEventIds().contains(existing)));
    }

    @Test
    void aCourseThatCannotStartStopsTheWholeLog() {
        LogVisitRequest request = aVisit().careItemCodes("DHPPI").homeCourses(aCourse("Metronidazole")).build();
        when(this.repository.findByPetIdAndClientId(PET, request.clientId())).thenReturn(Optional.empty());
        when(this.careApi.log(OWNER, PET, "DHPPI", TODAY, TODAY)).thenReturn(new CareEventDto(UUID.randomUUID(), "DHPPI", TODAY));
        when(this.medicationApi.start(eq(OWNER), eq(PET), any())).thenThrow(new InvalidCourse("name", InvalidCourse.REQUIRED));

        assertThatThrownBy(() -> this.service.log(OWNER, PET, request)).isInstanceOf(InvalidCourse.class);

        verify(this.repository, never()).saveAndFlush(any(VetVisit.class));
    }

    @Test
    void anInvalidVisitWritesNothing() {
        LogVisitRequest request = aVisit().justCheck().careItemCodes("DHPPI").build();
        when(this.repository.findByPetIdAndClientId(PET, request.clientId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.log(OWNER, PET, request)).isInstanceOf(InvalidVetVisit.class);

        verifyNoInteractions(this.careApi, this.petLogRecorder, this.medicationApi);
    }

    @Test
    void loggingAPlanKeepsItsEventAndTurnsItIntoAVisit() {
        VetVisit plan = plan();
        when(this.repository.findByIdAndPetId(plan.getId(), PET)).thenReturn(Optional.of(plan));

        VetVisitDto dto = this.service.logFromPlan(OWNER, PET, plan.getId(), plan.getVersion(), aVisit().justCheck().build());

        assertThat(dto.status()).isEqualTo(VisitStatus.DONE);
        assertThat(dto.outcomes()).containsExactly(VisitOutcome.FINE);
        assertThat(plan.getDayEventId()).isEqualTo(PLAN_EVENT);
        verify(this.dayEventApi, never()).cancel(any(), any());
    }

    @Test
    void loggingAVisitThatAlreadyHappenedIsAConflict() {
        VetVisit visit = done(UUID.randomUUID());
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> this.service.logFromPlan(OWNER, PET, visit.getId(), visit.getVersion(), aVisit().build()))
                .isInstanceOf(VisitStatusConflict.class);
    }

    @Test
    void loggingAPlanWithAStaleVersionConflicts() {
        VetVisit plan = plan();
        when(this.repository.findByIdAndPetId(plan.getId(), PET)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> this.service.logFromPlan(OWNER, PET, plan.getId(), plan.getVersion() + 1, aVisit().build()))
                .isInstanceOf(VersionConflict.class);
    }

    @Test
    void changingAVisitOnlyAddsItemsAndNeverUndoesOldOnes() {
        UUID oldCare = UUID.randomUUID();
        VetVisit visit = done(UUID.randomUUID());
        visit.getCareEventIds().add(oldCare);
        visit.getCourseIds().add(UUID.randomUUID());
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));
        when(this.careApi.events(eq(PET), any())).thenReturn(List.of(new CareEventDto(oldCare, "DHPPI", TODAY)));
        UUID newCare = UUID.randomUUID();
        when(this.careApi.log(OWNER, PET, "RABIES", TODAY, TODAY)).thenReturn(new CareEventDto(newCare, "RABIES", TODAY));

        this.service.update(OWNER, PET, visit.getId(), visit.getVersion(), aVisit().careItemCodes("DHPPI", "RABIES").build());

        assertThat(visit.getCareEventIds()).containsExactlyInAnyOrder(oldCare, newCare);
        assertThat(visit.getCourseIds()).hasSize(1);
        verify(this.careApi, never()).log(any(), any(), eq("DHPPI"), any(), any());
        verify(this.careApi, never()).undo(any(), any(), any(), any());
        verify(this.petLogRecorder, never()).delete(any(), any(), any(), any());
        verify(this.medicationApi, never()).stopOrDelete(any(), any(), any(), any(), anyInt());
    }

    @Test
    void aSecondWeightIsRefusedWhenTheVisitAlreadyHasOne() {
        VetVisit visit = done(UUID.randomUUID());
        visit.getLogs().put(UUID.randomUUID(), VisitLogRole.WEIGHT);
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> this.service.update(OWNER, PET, visit.getId(), visit.getVersion(), aVisit().weightGrams(4300).build()))
                .isInstanceOf(InvalidVetVisit.class)
                .hasMessage("weightGrams: " + InvalidVetVisit.ALREADY_LINKED);
    }

    @Test
    void aVisitWithTreatmentCannotBecomeJustACheck() {
        VetVisit visit = done(UUID.randomUUID());
        visit.getCourseIds().add(UUID.randomUUID());
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));

        assertThatThrownBy(() -> this.service.update(OWNER, PET, visit.getId(), visit.getVersion(), aVisit().justCheck().build()))
                .isInstanceOf(InvalidVetVisit.class);
    }

    @Test
    void changingTheProcedureAndTestsReplacesThem() {
        VetVisit visit = done(UUID.randomUUID());
        visit.getTests().put(VisitTestKind.URINE, false);
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));

        this.service.update(OWNER, PET, visit.getId(), visit.getVersion(), aVisit()
                .tests(new TestRequest(VisitTestKind.BLOOD, true))
                .procedure(new Procedure(VisitProcedureKind.DENTAL, null)).build());

        assertThat(visit.getTests()).containsOnlyKeys(VisitTestKind.BLOOD);
        assertThat(visit.getProcedures()).extracting("kind").containsExactly(VisitProcedureKind.DENTAL);
        assertThat(visit.getOutcomes()).containsExactlyInAnyOrder(VisitOutcome.TREATMENT, VisitOutcome.TESTS);
    }

    @Test
    void takingTheFollowUpOffRemovesItsPlanAndEvent() {
        VetVisit visit = done(UUID.randomUUID());
        VetVisit followUp = plan();
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));
        when(this.repository.findFirstByFollowUpOf(visit.getId())).thenReturn(Optional.of(followUp));

        this.service.update(OWNER, PET, visit.getId(), visit.getVersion(), aVisit().build());

        verify(this.dayEventApi).cancel(PET, PLAN_EVENT);
        verify(this.repository).delete(followUp);
        assertThat(visit.getOutcomes()).doesNotContain(VisitOutcome.BACK);
    }

    @Test
    void movingTheFollowUpReschedulesItsEvent() {
        VetVisit visit = done(UUID.randomUUID());
        VetVisit followUp = plan();
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));
        when(this.repository.findFirstByFollowUpOf(visit.getId())).thenReturn(Optional.of(followUp));

        this.service.update(OWNER, PET, visit.getId(), visit.getVersion(), aVisit().followUp(TODAY.plusDays(14), 660).build());

        assertThat(followUp.getVisitedOn()).isEqualTo(TODAY.plusDays(14));
        verify(this.dayEventApi).rescheduleVet(PET, PLAN_EVENT, TODAY.plusDays(14), 660, TODAY);
        verify(this.repository, never()).delete(any(VetVisit.class));
    }

    @Test
    void deletingWithItsItemsUndoesTheCareEventsLogsAndCourses() {
        UUID care = UUID.randomUUID();
        UUID log = UUID.randomUUID();
        UUID course = UUID.randomUUID();
        VetVisit visit = done(UUID.randomUUID());
        visit.setDayEventId(PLAN_EVENT);
        visit.getCareEventIds().add(care);
        visit.getLogs().put(log, VisitLogRole.CLINIC_MEDICINE);
        visit.getCourseIds().add(course);
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));
        when(this.careApi.events(eq(PET), any())).thenReturn(List.of(new CareEventDto(care, "DHPPI", TODAY)));
        when(this.petLogApi.snapshots(eq(PET), any())).thenReturn(List.of(
                new LogSnapshot(log, LogType.MEDICINE, null, null, null, Set.of(), TODAY, 600, null, null, "Convenia", null)));

        this.service.delete(OWNER, PET, visit.getId(), true, TODAY, 700);

        verify(this.careApi).undo(OWNER, PET, care, TODAY);
        verify(this.petLogRecorder).delete(OWNER, PET, log, TODAY);
        verify(this.medicationApi).stopOrDelete(OWNER, PET, course, TODAY, 700);
        verify(this.dayEventApi).cancel(PET, PLAN_EVENT);
        verify(this.repository).delete(visit);
    }

    @Test
    void deletingWithoutItsItemsLeavesThemAlone() {
        VetVisit visit = done(UUID.randomUUID());
        visit.getCareEventIds().add(UUID.randomUUID());
        visit.getLogs().put(UUID.randomUUID(), VisitLogRole.WEIGHT);
        visit.getCourseIds().add(UUID.randomUUID());
        when(this.repository.findByIdAndPetId(visit.getId(), PET)).thenReturn(Optional.of(visit));

        this.service.delete(OWNER, PET, visit.getId(), false, TODAY, null);

        verifyNoInteractions(this.petLogRecorder, this.medicationApi);
        verify(this.careApi, never()).undo(any(), any(), any(), any());
        verify(this.repository).delete(visit);
    }

    private static VetVisit done(UUID clientId) {
        return withId(VetVisit.done(PET, clientId, TODAY, "VetCare", NOW));
    }

    private static VetVisit plan() {
        VetVisit plan = withId(VetVisit.planned(PET, UUID.randomUUID(), TODAY.plusDays(7), 600, "VetCare", null, NOW));
        plan.setDayEventId(PLAN_EVENT);
        return plan;
    }

    private static VetVisit withId(VetVisit visit) {
        if (visit.getId() == null) {
            ReflectionTestUtils.setField(visit, "id", UUID.randomUUID());
        }
        return visit;
    }
}
