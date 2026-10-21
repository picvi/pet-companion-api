package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.domain.ActivityDisplayStatus;
import com.picvi.pet_companion_service.development.domain.AloneLadder;
import com.picvi.pet_companion_service.development.domain.ActivityEligibility;
import com.picvi.pet_companion_service.development.domain.ActivityRanking;
import com.picvi.pet_companion_service.development.domain.DevelopmentStage;
import com.picvi.pet_companion_service.development.domain.LifeStageCalculator;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.RankedActivity;
import com.picvi.pet_companion_service.development.domain.StageInfo;
import com.picvi.pet_companion_service.development.domain.StageProgress;
import com.picvi.pet_companion_service.development.domain.StageProgressCalculator;
import com.picvi.pet_companion_service.development.dto.DevelopmentActivityDto;
import com.picvi.pet_companion_service.development.dto.DevelopmentResponse;
import com.picvi.pet_companion_service.development.dto.NextStageDto;
import com.picvi.pet_companion_service.development.dto.StageDto;
import com.picvi.pet_companion_service.development.dto.StageProgressDto;
import com.picvi.pet_companion_service.development.dto.StageWindowDto;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DevelopmentService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final ActivityDataLoader activityDataLoader;
    private final LifeStageCalculator lifeStageCalculator;
    private final StageProgressCalculator stageProgressCalculator;
    private final ActivityRanking activityRanking;

    public DevelopmentResponse getDevelopment(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        if (date.isBefore(profile.birthDate())) {
            throw new InvalidDate();
        }

        List<ActivityDefinition> activities = this.activityDataLoader.loadActiveActivities();
        Map<String, ProgressState> progress = this.activityDataLoader.loadProgress(petId);

        StageInfo stage = this.lifeStageCalculator.calculate(profile.birthDate(), profile.sizeCategory(), date);
        StageProgress stageProgress = this.stageProgressCalculator.calculate(
                activities, progress, stage, profile.vaccinationStatus());

        List<ActivityDefinition> stageDefinitions = activities.stream()
                .filter(stage::overlaps)
                .toList();
        List<DevelopmentActivityDto> stageActivities = this.activityRanking
                .rank(stageDefinitions, progress, profile.vaccinationStatus(), stage.ageWeeks(), date).stream()
                .map(ranked -> this.toDto(ranked, profile, date))
                .toList();
        List<StageWindowDto> stageWindows = Arrays.stream(DevelopmentStage.values())
                .map(window -> new StageWindowDto(window, window.startWeek(profile.sizeCategory()),
                        window.endWeek(profile.sizeCategory())))
                .toList();

        return new DevelopmentResponse(
                date,
                stage.ageWeeks(),
                new StageDto(stage.stage(), stage.stageStartWeek(), stage.stageEndWeek(), stage.weeksLeftInStage()),
                stage.nextStage() == null ? null : new NextStageDto(stage.nextStage(), stage.nextStageStartWeek()),
                new StageProgressDto(stageProgress.completed(), stageProgress.total()),
                profile.vaccinationStatus(),
                stageActivities,
                stageWindows
        );
    }

    private DevelopmentActivityDto toDto(RankedActivity ranked, PetProfile profile, LocalDate date) {
        ActivityDefinition activity = ranked.activity();
        ProgressState state = ranked.state();
        boolean locked = ActivityEligibility.isLocked(activity, profile.vaccinationStatus());
        return new DevelopmentActivityDto(
                activity.code(),
                activity.kind(),
                activity.category(),
                activity.startWeek(),
                activity.endWeek(),
                activity.difficulty(),
                activity.vaccinationRequirement(),
                ActivityDisplayStatus.of(state.status(), locked),
                state.lastOutcome(),
                state.lastAttemptOn(),
                state.snoozedUntil(),
                activity.isRecurring(),
                ActivityEligibility.isRecurringDue(activity, state, date),
                activity.category().group(),
                ranked.section(),
                ranked.rank(),
                ranked.availableFrom(),
                AloneLadder.isAloneActivity(activity.code())
        );
    }
}
