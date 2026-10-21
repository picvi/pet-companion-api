package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.PlannerInput;
import com.picvi.pet_companion_service.development.domain.StageInfo;
import com.picvi.pet_companion_service.development.domain.TodayPlan;
import com.picvi.pet_companion_service.development.domain.TodayPlanner;
import com.picvi.pet_companion_service.development.dto.FocusDto;
import com.picvi.pet_companion_service.development.dto.PlanItemDto;
import com.picvi.pet_companion_service.development.dto.TodayResponse;
import com.picvi.pet_companion_service.development.dto.TodayStageDto;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TodayService {
    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final ActivityDataLoader activityDataLoader;
    private final TodayPlanner todayPlanner;

    public TodayResponse getToday(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        return this.buildToday(profile, date);
    }

    public TodayResponse buildToday(PetProfile profile, LocalDate date) {
        if (date.isBefore(profile.birthDate())) {
            throw new InvalidDate();
        }

        PlannerInput input = new PlannerInput(
                date,
                profile.birthDate(),
                profile.sizeCategory(),
                profile.createdOn(),
                profile.vaccinationStatus(),
                profile.vaccinationStatusChangedOn(),
                profile.vaccinationPromptCount(),
                this.activityDataLoader.loadActiveActivities(),
                this.activityDataLoader.loadProgress(profile.id())
        );
        return this.toResponse(profile, date, this.todayPlanner.plan(input));
    }

    private TodayResponse toResponse(PetProfile profile, LocalDate date, TodayPlan plan) {
        StageInfo stage = plan.stage();
        return new TodayResponse(
                date,
                profile.name(),
                stage.ageWeeks(),
                new TodayStageDto(stage.stage(), stage.weeksLeftInStage(), stage.weeksLeftInWindow(),
                        plan.stageProgress().completed(), plan.stageProgress().total()),
                FocusDto.of(plan.focus()),
                plan.plan().stream().map(PlanItemDto::of).toList(),
                plan.showWorriedCard(),
                plan.vaccinationPromptDue()
        );
    }
}
