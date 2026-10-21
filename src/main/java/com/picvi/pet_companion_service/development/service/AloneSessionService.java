package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.AloneLadder;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressRules;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.dto.ActivityProgressResponse;
import com.picvi.pet_companion_service.development.dto.AloneOverviewResponse;
import com.picvi.pet_companion_service.development.dto.AloneSessionDto;
import com.picvi.pet_companion_service.development.dto.AloneSessionResponse;
import com.picvi.pet_companion_service.development.dto.AloneSessionResponse.StepChange;
import com.picvi.pet_companion_service.development.entity.DevelopmentActivity;
import com.picvi.pet_companion_service.development.entity.PetActivityLog;
import com.picvi.pet_companion_service.development.entity.PetActivityProgress;
import com.picvi.pet_companion_service.development.repository.DevelopmentActivityRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityLogRepository;
import com.picvi.pet_companion_service.development.repository.PetActivityProgressRepository;
import com.picvi.pet_companion_service.exception.development.ActivityNotFound;
import com.picvi.pet_companion_service.exception.development.NotAnAloneActivity;
import com.picvi.pet_companion_service.exception.development.SessionNotFound;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AloneSessionService {
    public static final int DEFAULT_LIMIT = 7;

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final DevelopmentActivityRepository activityRepository;
    private final PetActivityProgressRepository progressRepository;
    private final PetActivityLogRepository logRepository;
    private final ProgressRules progressRules;

    public AloneSessionResponse record(UUID ownerId, UUID petId, String code, LocalDate date,
                                       int durationSeconds, Outcome reaction) {
        this.clientDateValidator.validate(date);
        AloneLadder.requireReaction(reaction);
        DevelopmentActivity activity = this.findAloneActivityOfOwnedPet(ownerId, petId, code);

        PetActivityLog log = this.logRepository.save(
                new PetActivityLog(this.petService.getReference(petId), activity, date, reaction, durationSeconds));
        this.rebuildProgress(petId, activity);

        StepChange change = this.changeOf(durationSeconds, AloneLadder.suggestNext(durationSeconds, reaction));
        return new AloneSessionResponse(toDto(log), change, this.overview(petId, activity, DEFAULT_LIMIT));
    }

    @Transactional(readOnly = true)
    public AloneOverviewResponse overview(UUID ownerId, UUID petId, String code, int limit) {
        DevelopmentActivity activity = this.findAloneActivityOfOwnedPet(ownerId, petId, code);
        return this.overview(petId, activity, limit);
    }

    public AloneOverviewResponse undo(UUID ownerId, UUID petId, String code, UUID sessionId) {
        DevelopmentActivity activity = this.findAloneActivityOfOwnedPet(ownerId, petId, code);
        PetActivityLog log = this.logRepository.findByIdAndPetIdAndActivityId(sessionId, petId, activity.getId())
                .orElseThrow(SessionNotFound::new);

        this.logRepository.delete(log);
        this.logRepository.flush();
        this.rebuildProgress(petId, activity);

        return this.overview(petId, activity, DEFAULT_LIMIT);
    }

    /** The progress summary is replayed from the sessions, so an undo can never leave it out of step. */
    private void rebuildProgress(UUID petId, DevelopmentActivity activity) {
        List<PetActivityLog> sessions = this.logRepository
                .findAllByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnAscCreatedAtAsc(petId, activity.getId());
        Optional<PetActivityProgress> existing = this.progressRepository.findByPetIdAndActivityId(petId, activity.getId());

        if (sessions.isEmpty()) {
            existing.ifPresent(this.progressRepository::delete);
            return;
        }

        ProgressState state = ProgressState.notStarted();
        for (PetActivityLog session : sessions) {
            Outcome outcome = AloneLadder.progressOutcome(activity.getCode(), session.getDurationSeconds(), session.getOutcome());
            state = this.progressRules.applyOutcome(state, activity.toDefinition(), outcome, session.getPerformedOn());
        }

        PetActivityProgress progress = existing
                .orElseGet(() -> new PetActivityProgress(this.petService.getReference(petId), activity));
        progress.apply(state);
        this.progressRepository.save(progress);
    }

    private AloneOverviewResponse overview(UUID petId, DevelopmentActivity activity, int limit) {
        List<PetActivityLog> latestFirst = this.logRepository
                .findByPetIdAndActivityIdAndDurationSecondsNotNullOrderByPerformedOnDescCreatedAtDesc(
                        petId, activity.getId(), PageRequest.of(0, limit));
        int suggested = latestFirst.isEmpty() ? AloneLadder.firstGoalSeconds()
                : AloneLadder.suggestNext(latestFirst.get(0).getDurationSeconds(), latestFirst.get(0).getOutcome());
        Integer longestCalm = this.logRepository
                .findFirstByPetIdAndActivityIdAndOutcomeAndDurationSecondsNotNullOrderByDurationSecondsDesc(
                        petId, activity.getId(), Outcome.CALM)
                .map(PetActivityLog::getDurationSeconds).orElse(null);

        List<AloneSessionDto> oldestFirst = new ArrayList<>(latestFirst.stream().map(AloneSessionService::toDto).toList());
        Collections.reverse(oldestFirst);

        ActivityProgressResponse progress = this.progressRepository.findByPetIdAndActivityId(petId, activity.getId())
                .map(found -> ActivityProgressResponse.of(activity.getCode(), found.toState()))
                .orElseGet(() -> ActivityProgressResponse.of(activity.getCode(), ProgressState.notStarted()));

        return new AloneOverviewResponse(activity.getCode(), AloneLadder.learnedAfterSeconds(activity.getCode()),
                suggested, longestCalm, oldestFirst, progress);
    }

    private StepChange changeOf(int doneSeconds, int nextSeconds) {
        int stepOfDone = AloneLadder.suggestNext(doneSeconds, Outcome.UNSURE);
        return Integer.compare(nextSeconds, stepOfDone) > 0 ? StepChange.UP
                : nextSeconds < stepOfDone ? StepChange.DOWN : StepChange.SAME;
    }

    private DevelopmentActivity findAloneActivityOfOwnedPet(UUID ownerId, UUID petId, String code) {
        this.petService.getOwnedPetProfile(ownerId, petId);
        DevelopmentActivity activity = this.activityRepository.findByCodeAndActiveTrue(code).orElseThrow(ActivityNotFound::new);
        if (!AloneLadder.isAloneActivity(code)) {
            throw new NotAnAloneActivity();
        }
        return activity;
    }

    private static AloneSessionDto toDto(PetActivityLog log) {
        return new AloneSessionDto(log.getId(), log.getPerformedOn(), log.getDurationSeconds(), log.getOutcome());
    }
}
