package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressRules;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;
import com.picvi.pet_companion_service.development.domain.VaccinationRequirement;
import com.picvi.pet_companion_service.development.dto.AloneOverviewResponse;
import com.picvi.pet_companion_service.development.dto.AloneSessionResponse;
import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import com.picvi.pet_companion_service.development.entity.PetActivityLog;
import com.picvi.pet_companion_service.development.entity.PetActivityProgress;
import com.picvi.pet_companion_service.development.repository.DevelopmentActivityRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityLogRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityProgressRepository;
import com.picvi.pet_companion_service.exception.development.InvalidOutcome;
import com.picvi.pet_companion_service.exception.development.NotAnAloneActivity;
import com.picvi.pet_companion_service.exception.development.SessionNotFound;
import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AloneSessionServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final String CODE = "ALONE_FIRST_SECONDS";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private DevelopmentActivityRepository activityRepository;
    @Mock
    private PetActivityProgressRepository progressRepository;
    @Mock
    private PetActivityLogRepository logRepository;

    private AloneSessionService service;
    private DevelopmentActivity activity;
    private PetActivityProgress stored;

    @BeforeEach
    void setUp() {
        service = new AloneSessionService(clientDateValidator, petService, activityRepository,
                progressRepository, logRepository, new ProgressRules());
        activity = activity(CODE);
        lenient().when(activityRepository.findByCodeAndActiveTrue(CODE)).thenReturn(Optional.of(activity));
        lenient().when(petService.getReference(PET_ID)).thenReturn(new Pet());
        lenient().when(logRepository.save(any(PetActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stored = null;
        lenient().when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L))
                .thenAnswer(invocation -> Optional.ofNullable(stored));
        lenient().when(progressRepository.save(any(PetActivityProgress.class))).thenAnswer(invocation -> {
            stored = invocation.getArgument(0);
            return stored;
        });
    }

    @Test
    void firstSessionIsSuggestedAtFiveSeconds() {
        givenNoSessions();

        AloneOverviewResponse overview = service.overview(OWNER_ID, PET_ID, CODE, 7);

        assertThat(overview.suggestedNextSeconds()).isEqualTo(5);
        assertThat(overview.sessions()).isEmpty();
        assertThat(overview.longestCalmSeconds()).isNull();
        assertThat(overview.progress().status()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    void calmSessionStoresTheReactionAndSecondsAndSuggestsTheNextStep() {
        PetActivityLog saved = session(DAY, 20, Outcome.CALM);
        givenSessions(List.of(saved));

        AloneSessionResponse response = service.record(OWNER_ID, PET_ID, CODE, DAY, 20, Outcome.CALM);

        ArgumentCaptor<PetActivityLog> logged = ArgumentCaptor.forClass(PetActivityLog.class);
        verify(logRepository).save(logged.capture());
        assertThat(logged.getValue().getDurationSeconds()).isEqualTo(20);
        assertThat(logged.getValue().getOutcome()).isEqualTo(Outcome.CALM);
        assertThat(response.overview().suggestedNextSeconds()).isEqualTo(30);
        assertThat(response.change()).isEqualTo(AloneSessionResponse.StepChange.UP);
        assertThat(response.overview().progress().status()).isEqualTo(ProgressStatus.IN_PROGRESS);
    }

    @Test
    void upsetSessionStepsBackDown() {
        givenSessions(List.of(session(DAY, 30, Outcome.SCARED)));

        AloneSessionResponse response = service.record(OWNER_ID, PET_ID, CODE, DAY, 30, Outcome.SCARED);

        assertThat(response.overview().suggestedNextSeconds()).isEqualTo(20);
        assertThat(response.change()).isEqualTo(AloneSessionResponse.StepChange.DOWN);
    }

    @Test
    void unsureSessionKeepsTheStep() {
        givenSessions(List.of(session(DAY, 30, Outcome.UNSURE)));

        AloneSessionResponse response = service.record(OWNER_ID, PET_ID, CODE, DAY, 30, Outcome.UNSURE);

        assertThat(response.overview().suggestedNextSeconds()).isEqualTo(30);
        assertThat(response.change()).isEqualTo(AloneSessionResponse.StepChange.SAME);
    }

    @Test
    void calmSessionPastTheThresholdCompletesTheActivityButStaysLoggable() {
        givenSessions(List.of(session(DAY, 60, Outcome.CALM)));

        AloneSessionResponse response = service.record(OWNER_ID, PET_ID, CODE, DAY, 60, Outcome.CALM);

        assertThat(response.overview().progress().status()).isEqualTo(ProgressStatus.COMPLETED);

        givenSessions(List.of(session(DAY, 60, Outcome.CALM), session(DAY.plusDays(1), 120, Outcome.CALM)));
        AloneSessionResponse next = service.record(OWNER_ID, PET_ID, CODE, DAY.plusDays(1), 120, Outcome.CALM);
        assertThat(next.overview().progress().status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(next.overview().suggestedNextSeconds()).isEqualTo(300);
    }

    @Test
    void overviewListsSessionsOldestFirstAndReportsLongestCalm() {
        PetActivityLog newest = session(DAY, 20, Outcome.SCARED);
        PetActivityLog older = session(DAY.minusDays(1), 30, Outcome.CALM);
        when(logRepository.findByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnDescCreatedAtDesc(
                eq(PET_ID), eq(1L), any(Pageable.class))).thenReturn(List.of(newest, older));
        when(logRepository.findFirstByPetIdAndActivityIdAndOutcomeAndDurationSecondsNotNullOrderByDurationSecondsDesc(
                PET_ID, 1L, Outcome.CALM)).thenReturn(Optional.of(older));

        AloneOverviewResponse overview = service.overview(OWNER_ID, PET_ID, CODE, 7);

        assertThat(overview.sessions()).extracting("durationSeconds").containsExactly(30, 20);
        assertThat(overview.longestCalmSeconds()).isEqualTo(30);
        assertThat(overview.suggestedNextSeconds()).isEqualTo(10);
    }

    @Test
    void undoRebuildsProgressFromTheRemainingSessions() {
        PetActivityLog undone = session(DAY, 20, Outcome.CALM);
        PetActivityLog earlier = session(DAY.minusDays(1), 10, Outcome.UNSURE);
        PetActivityProgress progress = new PetActivityProgress(new Pet(), activity);
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));
        when(logRepository.findByIdAndPetIdAndActivityId(undone.getId(), PET_ID, 1L)).thenReturn(Optional.of(undone));
        when(logRepository.findAllByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnAscCreatedAtAsc(PET_ID, 1L))
                .thenReturn(List.of(earlier));
        when(logRepository.findByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnDescCreatedAtDesc(
                eq(PET_ID), eq(1L), any(Pageable.class))).thenReturn(List.of(earlier));

        AloneOverviewResponse overview = service.undo(OWNER_ID, PET_ID, CODE, undone.getId());

        verify(logRepository).delete(undone);
        assertThat(progress.getAttemptDays()).isEqualTo(1);
        assertThat(progress.getLastAttemptOn()).isEqualTo(DAY.minusDays(1));
        assertThat(overview.suggestedNextSeconds()).isEqualTo(10);
    }

    @Test
    void undoOfTheOnlySessionRemovesProgress() {
        PetActivityLog only = session(DAY, 5, Outcome.CALM);
        PetActivityProgress progress = new PetActivityProgress(new Pet(), activity);
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));
        when(logRepository.findByIdAndPetIdAndActivityId(only.getId(), PET_ID, 1L)).thenReturn(Optional.of(only));
        givenNoSessions();

        AloneOverviewResponse overview = service.undo(OWNER_ID, PET_ID, CODE, only.getId());

        verify(progressRepository).delete(progress);
        assertThat(overview.suggestedNextSeconds()).isEqualTo(5);
    }

    @Test
    void unknownSessionIsNotFound() {
        UUID id = UUID.randomUUID();
        when(logRepository.findByIdAndPetIdAndActivityId(id, PET_ID, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.undo(OWNER_ID, PET_ID, CODE, id)).isInstanceOf(SessionNotFound.class);
    }

    @Test
    void otherActivitiesHaveNoSessions() {
        when(activityRepository.findByCodeAndActiveTrue("NAP_ROUTINE")).thenReturn(Optional.of(activity("NAP_ROUTINE")));

        assertThatThrownBy(() -> service.record(OWNER_ID, PET_ID, "NAP_ROUTINE", DAY, 20, Outcome.CALM))
                .isInstanceOf(NotAnAloneActivity.class);
        verify(logRepository, never()).save(any());
    }

    @Test
    void reactionIsRequiredToBeOneOfTheThree() {
        assertThatThrownBy(() -> service.record(OWNER_ID, PET_ID, CODE, DAY, 20, Outcome.PRACTISED))
                .isInstanceOf(InvalidOutcome.class);
        verify(logRepository, never()).save(any());
    }

    private void givenNoSessions() {
        givenSessions(List.of());
    }

    private void givenSessions(List<PetActivityLog> oldestFirst) {
        lenient().when(logRepository.findAllByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnAscCreatedAtAsc(PET_ID, 1L))
                .thenReturn(oldestFirst);
        lenient().when(logRepository.findByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnDescCreatedAtDesc(
                eq(PET_ID), eq(1L), any(Pageable.class))).thenReturn(oldestFirst.reversed());
        lenient().when(logRepository.findFirstByPetIdAndActivityIdAndOutcomeAndDurationSecondsNotNullOrderByDurationSecondsDesc(
                PET_ID, 1L, Outcome.CALM)).thenReturn(Optional.empty());
    }

    private PetActivityLog session(LocalDate day, int seconds, Outcome reaction) {
        PetActivityLog log = new PetActivityLog(new Pet(), activity, day, reaction, seconds);
        ReflectionTestUtils.setField(log, "id", UUID.randomUUID());
        return log;
    }

    private DevelopmentActivity activity(String code) {
        DevelopmentActivity entity = new DevelopmentActivity();
        ReflectionTestUtils.setField(entity, "id", 1L);
        ReflectionTestUtils.setField(entity, "code", code);
        ReflectionTestUtils.setField(entity, "kind", ActivityKind.SKILL);
        ReflectionTestUtils.setField(entity, "category", ActivityCategory.HOME_LIFE);
        ReflectionTestUtils.setField(entity, "startAgeWeeks", 8);
        ReflectionTestUtils.setField(entity, "endAgeWeeks", 16);
        ReflectionTestUtils.setField(entity, "difficulty", 1);
        ReflectionTestUtils.setField(entity, "vaccinationRequirement", VaccinationRequirement.NONE);
        ReflectionTestUtils.setField(entity, "sortOrder", 40);
        ReflectionTestUtils.setField(entity, "active", true);
        return entity;
    }
}
