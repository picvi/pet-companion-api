package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressRules;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;
import com.picvi.pet_companion_service.development.domain.SkipReason;
import com.picvi.pet_companion_service.development.domain.VaccinationRequirement;
import com.picvi.pet_companion_service.development.dto.ActivityProgressResponse;
import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import com.picvi.pet_companion_service.development.entity.PetActivityLog;
import com.picvi.pet_companion_service.development.entity.PetActivityProgress;
import com.picvi.pet_companion_service.development.repository.DevelopmentActivityRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityLogRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityProgressRepository;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.development.ActivityNotFound;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityProgressServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final String CODE = "PEOPLE_CALM_VISITOR";
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

    private ActivityProgressService service;
    private DevelopmentActivity activity;

    @BeforeEach
    void setUp() {
        service = new ActivityProgressService(clientDateValidator, petService, activityRepository,
                progressRepository, logRepository, new ProgressRules());
        activity = socialisationActivity();
    }

    @Test
    void someoneElsesPetIsNotFound() {
        when(petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> service.recordOutcome(OWNER_ID, PET_ID, CODE, Outcome.CALM, DAY))
                .isInstanceOf(PetNotFound.class);

        verify(progressRepository, never()).save(any());
    }

    @Test
    void unknownActivityCodeIsNotFound() {
        when(activityRepository.findByCodeAndActiveTrue("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordOutcome(OWNER_ID, PET_ID, "UNKNOWN", Outcome.CALM, DAY))
                .isInstanceOf(ActivityNotFound.class);
    }

    @Test
    void firstOutcomeCreatesProgressRow() {
        givenActivityExists();
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.empty());
        when(petService.getReference(PET_ID)).thenReturn(new Pet());
        when(progressRepository.save(any(PetActivityProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityProgressResponse response = service.recordOutcome(OWNER_ID, PET_ID, CODE, Outcome.CALM, DAY);

        ArgumentCaptor<PetActivityProgress> saved = ArgumentCaptor.forClass(PetActivityProgress.class);
        verify(progressRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(response.code()).isEqualTo(CODE);
        assertThat(response.status()).isEqualTo(ProgressStatus.COMPLETED);
        assertThat(response.completedOn()).isEqualTo(DAY);
    }

    @Test
    void outcomeIsAlsoWrittenToTheLog() {
        givenActivityExists();
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.empty());
        when(petService.getReference(PET_ID)).thenReturn(new Pet());
        when(progressRepository.save(any(PetActivityProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordOutcome(OWNER_ID, PET_ID, CODE, Outcome.UNSURE, DAY);

        ArgumentCaptor<PetActivityLog> logged = ArgumentCaptor.forClass(PetActivityLog.class);
        verify(logRepository).save(logged.capture());
        assertThat(logged.getValue().getOutcome()).isEqualTo(Outcome.UNSURE);
        assertThat(logged.getValue().getPerformedOn()).isEqualTo(DAY);
        assertThat(logged.getValue().getDurationSeconds()).isNull();
    }

    @Test
    void secondOutcomeOfTheSameDayReplacesTheLogEntry() {
        givenActivityExists();
        PetActivityLog earlier = new PetActivityLog(new Pet(), activity, DAY, Outcome.UNSURE, null);
        when(logRepository.findAllByPetIdAndActivityIdAndPerformedOn(PET_ID, 1L, DAY)).thenReturn(List.of(earlier));
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.empty());
        when(petService.getReference(PET_ID)).thenReturn(new Pet());
        when(progressRepository.save(any(PetActivityProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.recordOutcome(OWNER_ID, PET_ID, CODE, Outcome.CALM, DAY);

        verify(logRepository).deleteAll(List.of(earlier));
        verify(logRepository).save(any(PetActivityLog.class));
    }

    @Test
    void undoOfTheOnlyAttemptDeletesProgressRow() {
        givenActivityExists();
        PetActivityProgress progress = new PetActivityProgress(new Pet(), activity);
        progress.apply(new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null));
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));

        ActivityProgressResponse response = service.undo(OWNER_ID, PET_ID, CODE, DAY);

        verify(progressRepository).delete(progress);
        assertThat(response.status()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    void undoRestoresThePreviousOutcome() {
        givenActivityExists();
        LocalDate earlierDay = DAY.minusDays(2);
        PetActivityProgress progress = new PetActivityProgress(new Pet(), activity);
        progress.apply(new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 2, earlierDay, DAY, DAY, null));
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));
        when(logRepository.findFirstByPetIdAndActivityIdAndPerformedOnBeforeOrderByPerformedOnDescCreatedAtDesc(PET_ID, 1L, DAY))
                .thenReturn(Optional.of(new PetActivityLog(new Pet(), activity, earlierDay, Outcome.UNSURE, null)));
        when(progressRepository.save(progress)).thenReturn(progress);

        ActivityProgressResponse response = service.undo(OWNER_ID, PET_ID, CODE, DAY);

        assertThat(response.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(progress.getLastOutcome()).isEqualTo(Outcome.UNSURE);
        assertThat(progress.getLastAttemptOn()).isEqualTo(earlierDay);
        assertThat(progress.getAttemptDays()).isEqualTo(1);
        verify(progressRepository, never()).delete(any());
    }

    @Test
    void resetOfAnAloneActivityClearsEverySession() {
        DevelopmentActivity alone = socialisationActivity();
        ReflectionTestUtils.setField(alone, "code", "ALONE_FIRST_SECONDS");
        when(activityRepository.findByCodeAndActiveTrue("ALONE_FIRST_SECONDS")).thenReturn(Optional.of(alone));
        PetActivityProgress progress = new PetActivityProgress(new Pet(), alone);
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));

        ActivityProgressResponse response = service.undo(OWNER_ID, PET_ID, "ALONE_FIRST_SECONDS", DAY);

        verify(logRepository).deleteAllByPetIdAndActivityId(PET_ID, 1L);
        verify(progressRepository).delete(progress);
        assertThat(response.status()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    void outcomeCannotBeRecordedDirectlyForAnAloneActivity() {
        DevelopmentActivity alone = socialisationActivity();
        ReflectionTestUtils.setField(alone, "code", "ALONE_FIRST_SECONDS");
        when(activityRepository.findByCodeAndActiveTrue("ALONE_FIRST_SECONDS")).thenReturn(Optional.of(alone));

        assertThatThrownBy(() -> service.recordOutcome(OWNER_ID, PET_ID, "ALONE_FIRST_SECONDS", Outcome.PRACTISED, DAY))
                .isInstanceOf(com.picvi.pet_companion_service.exception.development.AloneActivityNeedsSessions.class);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void skipNotPossibleStoresHiddenStatus() {
        givenActivityExists();
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.empty());
        when(petService.getReference(PET_ID)).thenReturn(new Pet());
        when(progressRepository.save(any(PetActivityProgress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivityProgressResponse response = service.skip(OWNER_ID, PET_ID, CODE, SkipReason.NOT_POSSIBLE, DAY);

        assertThat(response.status()).isEqualTo(ProgressStatus.NOT_POSSIBLE);
    }

    @Test
    void restoreOfNeverAttemptedHiddenActivityDeletesRow() {
        givenActivityExists();
        PetActivityProgress progress = new PetActivityProgress(new Pet(), activity);
        progress.setStatus(ProgressStatus.NOT_POSSIBLE);
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));

        ActivityProgressResponse response = service.restore(OWNER_ID, PET_ID, CODE, DAY);

        verify(progressRepository).delete(progress);
        assertThat(response.status()).isEqualTo(ProgressStatus.NOT_STARTED);
    }

    @Test
    void restoreOfAttemptedHiddenActivityReturnsInProgress() {
        givenActivityExists();
        PetActivityProgress progress = new PetActivityProgress(new Pet(), activity);
        progress.setStatus(ProgressStatus.NOT_POSSIBLE);
        progress.setAttemptDays(2);
        when(progressRepository.findByPetIdAndActivityId(PET_ID, 1L)).thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);

        ActivityProgressResponse response = service.restore(OWNER_ID, PET_ID, CODE, DAY);

        assertThat(response.status()).isEqualTo(ProgressStatus.IN_PROGRESS);
        verify(progressRepository, never()).delete(any());
    }

    @Test
    void invalidDateStopsBeforeAnyLookup() {
        doThrow(new InvalidDate()).when(clientDateValidator).validate(DAY);

        assertThatThrownBy(() -> service.undo(OWNER_ID, PET_ID, CODE, DAY))
                .isInstanceOf(InvalidDate.class);

        verify(petService, never()).getOwnedPetProfile(any(), any());
    }

    private void givenActivityExists() {
        when(activityRepository.findByCodeAndActiveTrue(CODE)).thenReturn(Optional.of(activity));
    }

    private DevelopmentActivity socialisationActivity() {
        DevelopmentActivity entity = new DevelopmentActivity();
        ReflectionTestUtils.setField(entity, "id", 1L);
        ReflectionTestUtils.setField(entity, "code", CODE);
        ReflectionTestUtils.setField(entity, "kind", ActivityKind.SOCIALIZATION);
        ReflectionTestUtils.setField(entity, "category", ActivityCategory.PEOPLE);
        ReflectionTestUtils.setField(entity, "startAgeWeeks", 8);
        ReflectionTestUtils.setField(entity, "endAgeWeeks", 16);
        ReflectionTestUtils.setField(entity, "difficulty", 1);
        ReflectionTestUtils.setField(entity, "vaccinationRequirement", VaccinationRequirement.NONE);
        ReflectionTestUtils.setField(entity, "sortOrder", 10);
        ReflectionTestUtils.setField(entity, "active", true);
        return entity;
    }
}
