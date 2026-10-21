package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.ChangePhase;
import com.picvi.pet_companion_service.nutrition.domain.ChangeView;
import com.picvi.pet_companion_service.nutrition.domain.ReactionKind;
import com.picvi.pet_companion_service.nutrition.domain.ReactionSignal;
import com.picvi.pet_companion_service.nutrition.domain.ReactionSignal.Kind;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.DayMix;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.domain.TummyReaction;
import com.picvi.pet_companion_service.nutrition.domain.TummyReactionPolicy;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.LogPredicates;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import com.picvi.pet_companion_service.rhythm.api.RhythmApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reads the tummy logs of a running food change and reacts: slows the step down, or pauses the plan. It runs after a log is
 * saved and again when the diet is read, so a reaction lost with the in-memory event is still applied. Both ways are
 * idempotent: a step is slowed down once, and the plan is paused once per step.
 */
@Service
@RequiredArgsConstructor
public class TummyReactionService {
    private final PetLogApi petLogApi;
    private final TummyReactionPolicy policy;
    private final FoodChangeReader changeReader;
    private final FoodChangeActions actions;
    private final FoodTransitionRepository transitionRepository;
    private final FoodTransitionExtensionRepository extensionRepository;
    private final FoodTransitionPauseRepository pauseRepository;
    private final RhythmApi rhythmApi;

    /** The reaction a running change is in on this day, if any. Changes nothing. */
    @Transactional(readOnly = true)
    public Optional<TummyReaction> reactionOn(FoodTransition transition, LocalDate date, int mealsPerDay) {
        return this.evaluate(transition, date, mealsPerDay).map(Evaluation::reaction);
    }

    /** Which step the reaction belongs to (for the app), or empty when there is none. */
    @Transactional(readOnly = true)
    public Optional<Integer> stepOf(FoodTransition transition, LocalDate date, int mealsPerDay) {
        return this.evaluate(transition, date, mealsPerDay).map(Evaluation::stepOrder);
    }

    @Transactional
    public void apply(UUID petId, LocalDate date) {
        Optional<FoodTransition> open = this.transitionRepository.findOpen(petId, List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED))
                .filter(transition -> transition.getStatus() == TransitionStatus.ACTIVE);
        if (open.isEmpty()) {
            return;
        }
        FoodTransition transition = open.get();
        Optional<Evaluation> evaluation = this.evaluate(transition, date, this.rhythmApi.mealsPerDay(petId, date));
        if (evaluation.isEmpty()) {
            return;
        }
        TummyReaction reaction = evaluation.get().reaction();
        if (reaction.kind() == ReactionKind.EXTEND_STEP) {
            this.actions.slowDown(transition, date, reaction.logId());
        } else if (reaction.kind() == ReactionKind.PAUSE_AND_VET && !this.pausedSince(transition, evaluation.get().stepFrom())) {
            this.actions.pause(transition, date);
            this.transitionRepository.saveAndFlush(transition);
        }
    }

    private record Evaluation(TummyReaction reaction, int stepOrder, LocalDate stepFrom) {
    }

    private Optional<Evaluation> evaluate(FoodTransition transition, LocalDate date, int mealsPerDay) {
        if (!transition.isOpen()) {
            return Optional.empty();
        }
        ChangeView view = this.changeReader.view(transition, date, mealsPerDay);
        if (view.phaseOn(date) != ChangePhase.RUNNING) {
            return Optional.empty();
        }
        DayMix mix = view.plan().mixOn(date).orElseThrow();
        LocalDate stepFrom = mix.step().from();
        UUID petId = transition.getPetId();

        List<ReactionSignal> stepSignals = this.petLogApi.logsSince(petId, stepFrom).stream()
                .flatMap(log -> signalsOf(log).stream())
                .toList();
        List<ReactionSignal> lastMeals = this.petLogApi.lastMeals(petId, TummyReactionPolicy.REFUSED_MEALS_IN_A_ROW).stream()
                .filter(meal -> !meal.localDate().isBefore(transition.getStartOn()))
                .flatMap(meal -> signalsOf(meal).stream())
                .toList();

        TummyReaction reaction = this.policy.evaluate(stepSignals, lastMeals);
        if (reaction.kind() == ReactionKind.EXTEND_STEP && this.wasUndone(transition, mix.step().order())) {
            reaction = TummyReaction.NONE;
        }
        return reaction.kind() == ReactionKind.NONE ? Optional.empty()
                : Optional.of(new Evaluation(reaction, mix.step().order(), stepFrom));
    }

    private boolean wasUndone(FoodTransition transition, int stepOrder) {
        return this.extensionRepository.findByTransitionIdAndStepOrder(transition.getId(), stepOrder)
                .map(extension -> extension.isTombstone())
                .orElse(false);
    }

    private boolean pausedSince(FoodTransition transition, LocalDate stepFrom) {
        return this.pauseRepository.findAllByTransitionIdOrderByPausedOnAsc(transition.getId()).stream()
                .anyMatch(pause -> !pause.getPausedOn().isBefore(stepFrom));
    }

    static List<ReactionSignal> signalsOf(LogSnapshot log) {
        java.util.ArrayList<ReactionSignal> signals = new java.util.ArrayList<>();
        if (log.type() == LogType.TOILET) {
            if (log.detail() != LogDetail.PEE && LogPredicates.looseStool(log)) {
                signals.add(new ReactionSignal(log.logId(), Kind.LOOSE_STOOL));
            }
            if (log.flags().contains(LogFlag.BLOOD)) {
                signals.add(new ReactionSignal(log.logId(), Kind.BLOOD));
            }
        } else if (log.type() == LogType.SYMPTOM) {
            if (LogPredicates.vomiting(log)) {
                signals.add(new ReactionSignal(log.logId(), Kind.VOMITING));
            }
            if (log.wellbeing() == Wellbeing.VERY_UNWELL) {
                signals.add(new ReactionSignal(log.logId(), Kind.VERY_UNWELL));
            }
        } else if (log.type() == LogType.MEAL) {
            signals.add(new ReactionSignal(log.logId(), log.detail() == LogDetail.REFUSED ? Kind.MEAL_REFUSED : Kind.MEAL_EATEN));
        }
        return signals;
    }
}
