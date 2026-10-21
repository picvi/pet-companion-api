package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.exception.nutrition.FoodChangeNotActive;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodChange;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.DayMix;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionExtension;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPause;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import com.picvi.pet_companion_service.rhythm.api.RhythmApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/** The small writes on a running food change: slow a step down, pause, resume. Used by the owner's actions and by the tummy reaction. */
@Component
@RequiredArgsConstructor
public class FoodChangeActions {
    public static final int SLOW_DOWN_DAYS = 2;

    private final RhythmApi rhythmApi;
    private final FoodChangeReader changeReader;
    private final PetFoodRepository foodRepository;
    private final FoodTransitionExtensionRepository extensionRepository;
    private final FoodTransitionPauseRepository pauseRepository;
    private final Clock clock;

    /**
     * Slows down the step the plan is on today. A step is slowed down once. An automatic slow-down ({@code reasonLogId} set)
     * never overrides an existing or undone one; the owner's own request revives an undone one.
     *
     * @return true when days were added
     */
    public boolean slowDown(FoodTransition transition, LocalDate date, UUID reasonLogId) {
        if (transition.getStatus() != TransitionStatus.ACTIVE) {
            throw new FoodChangeNotActive();
        }
        int mealsPerDay = this.rhythmApi.mealsPerDay(transition.getPetId(), date);
        PetFood from = this.foodRepository.findById(transition.getFromFoodId()).orElseThrow();
        PetFood to = this.foodRepository.findById(transition.getToFoodId()).orElseThrow();
        DayMix mix = this.changeReader.plan(transition, from, to, date, mealsPerDay).mixOn(date)
                .orElseThrow(() -> new InvalidFoodChange("The plan is not on a step today"));
        int step = mix.step().order();

        Optional<FoodTransitionExtension> existing = this.extensionRepository.findByTransitionIdAndStepOrder(transition.getId(), step);
        if (existing.isPresent()) {
            if (reasonLogId == null && existing.get().isTombstone()) {
                existing.get().restore(SLOW_DOWN_DAYS);
                this.extensionRepository.saveAndFlush(existing.get());
                return true;
            }
            return false;
        }
        this.extensionRepository.saveAndFlush(new FoodTransitionExtension(transition.getId(), step, SLOW_DOWN_DAYS, reasonLogId,
                OffsetDateTime.now(this.clock)));
        return true;
    }

    /** Takes back the latest slow-down: an automatic one is kept as an undone marker, the owner's own one is deleted. */
    public void undoExtension(FoodTransition transition) {
        this.extensionRepository.findAllByTransitionId(transition.getId()).stream()
                .filter(extension -> !extension.isTombstone())
                .max(java.util.Comparator.comparing(FoodTransitionExtension::getCreatedAt).thenComparing(FoodTransitionExtension::getId))
                .ifPresent(extension -> {
                    if (extension.getReasonLogId() != null) {
                        extension.tombstone();
                        this.extensionRepository.save(extension);
                    } else {
                        this.extensionRepository.delete(extension);
                    }
                });
    }

    public void pause(FoodTransition transition, LocalDate date) {
        if (transition.getStatus() == TransitionStatus.PAUSED) {
            return;
        }
        this.pauseRepository.save(new FoodTransitionPause(transition.getId(), date));
        transition.pause();
    }

    public void resume(FoodTransition transition, LocalDate date) {
        if (transition.getStatus() != TransitionStatus.PAUSED) {
            return;
        }
        this.endOpenPause(transition, date);
        transition.resume();
    }

    public void endOpenPause(FoodTransition transition, LocalDate date) {
        this.pauseRepository.findFirstByTransitionIdAndResumedOnIsNull(transition.getId()).ifPresent(pause -> {
            pause.resumeOn(date);
            this.pauseRepository.save(pause);
        });
    }
}
