package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressRules;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;
import com.picvi.pet_companion_service.development.domain.SkipReason;
import com.picvi.pet_companion_service.development.dto.ActivityProgressResponse;
import com.picvi.pet_companion_service.development.dto.ActivityTimelineRow;
import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import com.picvi.pet_companion_service.development.entity.PetActivityLog;
import com.picvi.pet_companion_service.development.entity.PetActivityProgress;
import com.picvi.pet_companion_service.development.repository.DevelopmentActivityRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityLogRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityProgressRepository;
import com.picvi.pet_companion_service.development.domain.AloneLadder;
import com.picvi.pet_companion_service.exception.development.ActivityNotFound;
import com.picvi.pet_companion_service.exception.development.AloneActivityNeedsSessions;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ActivityProgressService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final DevelopmentActivityRepository activityRepository;
    private final PetActivityProgressRepository progressRepository;
    private final PetActivityLogRepository logRepository;
    private final ProgressRules progressRules;

    public ActivityProgressResponse recordOutcome(UUID ownerId, UUID petId, String code, Outcome outcome, LocalDate date) {
        DevelopmentActivity activity = this.findActivityOfOwnedPet(ownerId, petId, code, date);
        requireNotAlone(code);
        PetActivityProgress progress = this.findOrCreateProgress(petId, activity);

        progress.apply(this.progressRules.applyOutcome(progress.toState(), activity.toDefinition(), outcome, date));
        this.replaceLogOfTheDay(petId, activity, outcome, date);

        return this.toResponse(this.progressRepository.save(progress));
    }

    @Transactional(readOnly = true)
    public List<ActivityTimelineRow> timelinePage(UUID petId, LocalDate date, boolean includeWholeDay, UUID idBefore, int limit) {
        return this.logRepository.findTimelinePage(petId, date, includeWholeDay, idBefore, PageRequest.of(0, limit + 1)).stream()
                .map(ActivityTimelineRow::of)
                .toList();
    }

    public ActivityProgressResponse skip(UUID ownerId, UUID petId, String code, SkipReason reason, LocalDate date) {
        DevelopmentActivity activity = this.findActivityOfOwnedPet(ownerId, petId, code, date);
        PetActivityProgress progress = this.findOrCreateProgress(petId, activity);

        progress.apply(this.progressRules.applySkip(progress.toState(), reason, date));

        return this.toResponse(this.progressRepository.save(progress));
    }

    public ActivityProgressResponse restore(UUID ownerId, UUID petId, String code, LocalDate date) {
        DevelopmentActivity activity = this.findActivityOfOwnedPet(ownerId, petId, code, date);
        Optional<PetActivityProgress> existing = this.progressRepository.findByPetIdAndActivityId(petId, activity.getId());
        if (existing.isEmpty()) {
            return this.notStartedResponse(code);
        }

        PetActivityProgress progress = existing.get();
        ProgressState restored = this.progressRules.restore(progress.toState());
        if (restored.status() == ProgressStatus.NOT_STARTED && restored.attemptDays() == 0) {
            this.progressRepository.delete(progress);
            return this.notStartedResponse(code);
        }

        progress.apply(restored);
        return this.toResponse(this.progressRepository.save(progress));
    }

    public ActivityProgressResponse undo(UUID ownerId, UUID petId, String code, LocalDate date) {
        DevelopmentActivity activity = this.findActivityOfOwnedPet(ownerId, petId, code, date);
        Optional<PetActivityProgress> existing = this.progressRepository.findByPetIdAndActivityId(petId, activity.getId());
        if (AloneLadder.isAloneActivity(code)) {
            // A reset of an alone-time activity clears every session; single sessions are undone one by one.
            this.logRepository.deleteAllByPetIdAndActivityId(petId, activity.getId());
            existing.ifPresent(this.progressRepository::delete);
            return this.notStartedResponse(code);
        }
        if (existing.isEmpty()) {
            return this.notStartedResponse(code);
        }

        PetActivityProgress progress = existing.get();
        this.logRepository.deleteAll(this.logRepository.findAllByPetIdAndActivityIdAndPerformedOn(petId, activity.getId(), date));

        ProgressState before = progress.toState().asOfStartOf(date);
        if (before.attemptDays() == 0) {
            this.progressRepository.delete(progress);
            return this.notStartedResponse(code);
        }

        // Earlier attempts survive an undo: the previous log entry becomes the last outcome again.
        Optional<PetActivityLog> previous = this.logRepository
                .findFirstByPetIdAndActivityIdAndPerformedOnBeforeOrderByPerformedOnDescCreatedAtDesc(petId, activity.getId(), date);
        ProgressState restored = new ProgressState(before.status(),
                previous.map(PetActivityLog::getOutcome).orElse(null), before.attemptDays(), before.firstAttemptOn(),
                previous.map(PetActivityLog::getPerformedOn).orElse(null), before.completedOn(), before.snoozedUntil());

        progress.apply(restored);
        return this.toResponse(this.progressRepository.save(progress));
    }

    private DevelopmentActivity findActivityOfOwnedPet(UUID ownerId, UUID petId, String code, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        return this.activityRepository.findByCodeAndActiveTrue(code).orElseThrow(ActivityNotFound::new);
    }

    private static void requireNotAlone(String code) {
        if (AloneLadder.isAloneActivity(code)) {
            throw new AloneActivityNeedsSessions();
        }
    }

    private void replaceLogOfTheDay(UUID petId, DevelopmentActivity activity, Outcome outcome, LocalDate date) {
        this.logRepository.deleteAll(this.logRepository.findAllByPetIdAndActivityIdAndPerformedOn(petId, activity.getId(), date));
        this.logRepository.save(new PetActivityLog(this.petService.getReference(petId), activity, date, outcome, null));
    }

    private PetActivityProgress findOrCreateProgress(UUID petId, DevelopmentActivity activity) {
        return this.progressRepository.findByPetIdAndActivityId(petId, activity.getId())
                .orElseGet(() -> new PetActivityProgress(this.petService.getReference(petId), activity));
    }

    private ActivityProgressResponse toResponse(PetActivityProgress progress) {
        return ActivityProgressResponse.of(progress.getActivity().getCode(), progress.toState());
    }

    private ActivityProgressResponse notStartedResponse(String code) {
        return ActivityProgressResponse.of(code, ProgressState.notStarted());
    }
}
