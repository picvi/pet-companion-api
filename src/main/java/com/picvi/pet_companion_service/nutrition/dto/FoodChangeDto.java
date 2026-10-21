package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.ChangeView;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.DayMix;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** The running (or just finished) food change with all its steps, so the app can show today's step without a calculator of its own. */
public record FoodChangeDto(UUID id,
                            TransitionStatus status,
                            TransitionPace pace,
                            UUID fromFoodId,
                            UUID toFoodId,
                            LocalDate startOn,
                            LocalDate endOn,
                            Integer dayOfPlan,
                            Integer totalDays,
                            List<PlanStepDto> steps,
                            ReactionDto reaction) {

    public static FoodChangeDto of(ChangeView view, LocalDate date, ReactionDto reaction) {
        var transition = view.transition();
        if (view.plan() == null) {
            return new FoodChangeDto(transition.getId(), transition.getStatus(), transition.getPace(), transition.getFromFoodId(),
                    transition.getToFoodId(), transition.getStartOn(), transition.getEndedOn(), null, null, List.of(), null);
        }
        DayMix mix = view.plan().mixOn(date).orElse(null);
        return new FoodChangeDto(transition.getId(), transition.getStatus(), transition.getPace(), transition.getFromFoodId(),
                transition.getToFoodId(), transition.getStartOn(), view.plan().lastDay(),
                mix == null ? null : mix.dayOfPlan(), view.plan().totalDays(),
                view.plan().steps().stream().map(PlanStepDto::of).toList(), reaction);
    }
}
