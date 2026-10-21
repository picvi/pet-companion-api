package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.ChangeView;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PaceStep;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PauseInterval;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.Plan;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanInput;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionExtension;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPaceRow;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPause;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPaceRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Loads a pet's food change and builds its plan. Read-only except {@link #closeIfFinished}. */
@Component
@RequiredArgsConstructor
public class FoodChangeReader {
    static final String ACTIVE = "ACTIVE";

    private final FoodTransitionRepository transitionRepository;
    private final FoodTransitionExtensionRepository extensionRepository;
    private final FoodTransitionPauseRepository pauseRepository;
    private final FoodTransitionPaceRepository paceRepository;
    private final PetFoodRepository foodRepository;
    private final TransitionPlanCalculator calculator;

    /** The change that matters on {@code asOf}: a running or paused one, or one that finished and has not been acknowledged. */
    public Optional<ChangeView> current(UUID petId, LocalDate asOf, int mealsPerDay) {
        return this.transitionRepository.findOpen(petId, List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED))
                .or(() -> this.transitionRepository.findFinishedUnseen(petId, TransitionStatus.DONE))
                .map(transition -> this.view(transition, asOf, mealsPerDay));
    }

    public Optional<ChangeView> open(UUID petId, LocalDate asOf, int mealsPerDay) {
        return this.transitionRepository.findOpen(petId, List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED))
                .map(transition -> this.view(transition, asOf, mealsPerDay));
    }

    public ChangeView view(FoodTransition transition, LocalDate asOf, int mealsPerDay) {
        PetFood from = this.food(transition.getFromFoodId());
        PetFood to = this.food(transition.getToFoodId());
        Plan plan = transition.isOpen() ? this.plan(transition, from, to, asOf, mealsPerDay) : null;
        return new ChangeView(transition, plan, from, to);
    }

    public Plan plan(FoodTransition transition, PetFood from, PetFood to, LocalDate asOf, int mealsPerDay) {
        Integer totalDaily = from.getDailyGrams() != null ? from.getDailyGrams() : to.getDailyGrams();
        Map<Integer, Integer> extensions = new HashMap<>();
        for (FoodTransitionExtension extension : this.extensionRepository.findAllByTransitionId(transition.getId())) {
            extensions.merge(extension.getStepOrder(), extension.getDays(), Integer::sum);
        }
        List<PauseInterval> pauses = this.pauseRepository.findAllByTransitionIdOrderByPausedOnAsc(transition.getId()).stream()
                .map(pause -> new PauseInterval(pause.getPausedOn(), pause.getResumedOn()))
                .toList();
        return this.calculator.plan(new PlanInput(this.paceSteps(transition.getPace().name()), transition.getStartOn(), pauses,
                extensions, MealPortionCalculator.perMealGrams(totalDaily, mealsPerDay)), asOf);
    }

    public List<PaceStep> paceSteps(String pace) {
        return this.paceRepository.findAllByPaceAndStatusOrderByStepOrderAsc(pace, ACTIVE).stream()
                .map((FoodTransitionPaceRow row) -> new PaceStep(row.getStepOrder(), row.getDays(), row.getNewPercent()))
                .toList();
    }

    /**
     * Done on read: the first read after the last day closes the change. The old food stops being given on the last day
     * (the new food is the whole meal then); the owner sees "all done" once.
     */
    public void closeIfFinished(UUID petId, LocalDate date, int mealsPerDay) {
        this.transitionRepository.findOpen(petId, List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED))
                .filter(transition -> transition.getStatus() == TransitionStatus.ACTIVE)
                .ifPresent(transition -> {
                    PetFood from = this.food(transition.getFromFoodId());
                    PetFood to = this.food(transition.getToFoodId());
                    Plan plan = this.plan(transition, from, to, date, mealsPerDay);
                    if (plan.isOver(date)) {
                        transition.finish(plan.lastDay());
                        from.endOn(plan.lastDay());
                        this.transitionRepository.saveAndFlush(transition);
                        this.foodRepository.saveAndFlush(from);
                    }
                });
    }

    private PetFood food(UUID id) {
        return this.foodRepository.findById(id).orElseThrow();
    }
}
