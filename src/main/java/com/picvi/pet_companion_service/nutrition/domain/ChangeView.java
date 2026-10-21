package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.Plan;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;

import java.time.LocalDate;
import java.util.UUID;

/** A food change as it looks on one day: the stored transition and its computed plan (null once the plan is closed). */
public record ChangeView(FoodTransition transition, Plan plan, PetFood fromFood, PetFood toFood) {

    public UUID id() {
        return this.transition.getId();
    }

    public boolean isFinishedAndUnseen() {
        return this.transition.getStatus() == TransitionStatus.DONE;
    }

    public ChangePhase phaseOn(LocalDate date) {
        if (this.plan == null || date.isBefore(this.transition.getStartOn())) {
            return ChangePhase.BEFORE_START;
        }
        return this.plan.isOver(date) ? ChangePhase.OVER : ChangePhase.RUNNING;
    }
}
