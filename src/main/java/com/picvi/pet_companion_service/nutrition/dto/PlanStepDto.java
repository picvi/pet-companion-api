package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanStep;

import java.time.LocalDate;

public record PlanStepDto(int order, LocalDate from, LocalDate to, int newPercent, Integer oldGrams, Integer newGrams, int extendedDays) {

    public static PlanStepDto of(PlanStep step) {
        return new PlanStepDto(step.order(), step.from(), step.to(), step.newPercent(), step.oldGrams(), step.newGrams(), step.extendedDays());
    }
}
