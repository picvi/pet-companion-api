package com.picvi.pet_companion_service.nutrition.api;

import com.picvi.pet_companion_service.nutrition.domain.ChangePhase;
import com.picvi.pet_companion_service.nutrition.domain.ChangeView;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.PortionState;
import com.picvi.pet_companion_service.nutrition.domain.TransitionEndReason;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.DayMix;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionExtension;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPause;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductChangeRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import com.picvi.pet_companion_service.nutrition.service.DietHistoryService;
import com.picvi.pet_companion_service.nutrition.service.DietNotices;
import com.picvi.pet_companion_service.nutrition.service.FoodChangeReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/** What other modules (rhythm, pet log, timeline, brief) may ask nutrition. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NutritionApi {
    private final PetFoodRepository foodRepository;
    private final MealPortionCalculator calculator;
    private final DietNotices notices;
    private final FoodChangeReader changeReader;
    private final FoodTransitionRepository transitionRepository;
    private final FoodTransitionExtensionRepository extensionRepository;
    private final FoodTransitionPauseRepository pauseRepository;
    private final DietHistoryService historyService;
    private final FoodProductRepository productRepository;
    private final FoodProductChangeRepository productChangeRepository;

    public MealPlan portionsFor(UUID petId, LocalDate date, int mealsPerDay) {
        Optional<ChangeView> change = this.changeReader.current(petId, date, mealsPerDay);
        DayPortions today = this.dayPortions(petId, date, mealsPerDay, change);
        DayPortions tomorrow = this.dayPortions(petId, date.plusDays(1), mealsPerDay, change);
        Optional<DayMix> mix = change.filter(view -> view.plan() != null).flatMap(view -> view.plan().mixOn(date));
        return new MealPlan(today, tomorrow, mix.map(DayMix::dayOfPlan).orElse(null), mix.map(DayMix::totalDays).orElse(null));
    }

    /** Removes the person from the shared food catalogue; the foods stay. */
    @Transactional
    public void forgetUser(UUID userId) {
        this.productRepository.clearCreatedBy(userId);
        this.productRepository.clearReviewedBy(userId);
        this.productChangeRepository.clearChangedBy(userId);
    }

    public List<ActiveFood> activeFoods(UUID petId, LocalDate date) {
        return this.foodRepository.findGivenOn(petId, date, PetFoodStatus.ENTERED_IN_ERROR).stream()
                .map(food -> new ActiveFood(food.getId(), food.getProduct().getId(), food.getKind()))
                .toList();
    }

    /**
     * A meal is a "new food" meal when a change covers the day and the meal has the incoming food. A meal that
     * lists no food during a change counts too: the dog was eating the mix.
     */
    public boolean isNewFoodMeal(UUID petId, LocalDate date, List<UUID> mealFoodIds) {
        return this.transitionRepository.findAllByPetIdOrderByStartOnDesc(petId).stream()
                .filter(transition -> this.coversDay(transition, date))
                .anyMatch(transition -> mealFoodIds.isEmpty() || mealFoodIds.contains(transition.getToFoodId()));
    }

    /** Days since the latest food-change plan began on or before the given day. */
    public OptionalInt daysSinceFoodChange(UUID petId, LocalDate date) {
        return this.transitionRepository.findAllByPetIdOrderByStartOnDesc(petId).stream()
                .map(FoodTransition::getStartOn)
                .filter(startedOn -> !startedOn.isAfter(date))
                .findFirst()
                .map(startedOn -> OptionalInt.of(Math.toIntExact(ChronoUnit.DAYS.between(startedOn, date))))
                .orElseGet(OptionalInt::empty);
    }

    /** The latest food change if it is running or paused on the day, or started within the given days before it. */
    public Optional<FoodChangeNow> changeNow(UUID petId, LocalDate date, int lookbackDays) {
        return this.transitionRepository.findAllByPetIdOrderByStartOnDesc(petId).stream()
                .filter(transition -> !transition.getStartOn().isAfter(date))
                .findFirst()
                .map(transition -> new FoodChangeNow(this.stateOn(transition, date), transition.getStartOn()))
                .filter(change -> change.state() != FoodChangeNow.State.ENDED
                        || !change.startedOn().isBefore(date.minusDays(lookbackDays - 1L)));
    }

    private FoodChangeNow.State stateOn(FoodTransition transition, LocalDate date) {
        if (transition.getStatus() == TransitionStatus.PAUSED) {
            return FoodChangeNow.State.PAUSED;
        }
        boolean running = transition.getEndReason() != TransitionEndReason.REPLACED && this.coversDay(transition, date);
        return running ? FoodChangeNow.State.RUNNING : FoodChangeNow.State.ENDED;
    }

    private boolean coversDay(FoodTransition transition, LocalDate date) {
        if (date.isBefore(transition.getStartOn()) || transition.getEndReason() == TransitionEndReason.REPLACED) {
            return false;
        }
        if (transition.isOpen()) {
            return !this.changeReader.view(transition, date, 1).plan().isOver(date);
        }
        return transition.getStatus() == TransitionStatus.DONE ? !date.isAfter(transition.getEndedOn()) : date.isBefore(transition.getEndedOn());
    }

    /** What the dog ate between two days, for the vet brief. */
    public DietHistoryResponse dietHistory(UUID petId, LocalDate from, LocalDate to) {
        return this.historyService.history(petId, from, to);
    }

    /** Events of every food change of a pet, newest first, for the timeline. */
    public List<FoodChangeEvent> changes(UUID petId) {
        List<FoodTransition> transitions = this.transitionRepository.findAllByPetIdOrderByStartOnDesc(petId);
        if (transitions.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = transitions.stream().map(FoodTransition::getId).toList();
        List<FoodChangeEvent> events = new ArrayList<>();
        for (FoodTransition transition : transitions) {
            if (transition.getEndReason() == TransitionEndReason.REPLACED) {
                events.add(event(transition, FoodChangeEvent.Kind.ALL_AT_ONCE, transition.getStartOn(), 0));
                continue;
            }
            events.add(event(transition, FoodChangeEvent.Kind.STARTED, transition.getStartOn(), 0));
            if (transition.getStatus() == TransitionStatus.DONE) {
                events.add(event(transition, FoodChangeEvent.Kind.FINISHED, transition.getEndedOn(), 0));
            } else if (transition.getStatus() == TransitionStatus.CANCELLED) {
                events.add(event(transition, FoodChangeEvent.Kind.CANCELLED, transition.getEndedOn(), 0));
            }
        }
        for (FoodTransitionExtension extension : this.extensionRepository.findAllByTransitionIdIn(ids)) {
            if (extension.isTombstone()) {
                continue;
            }
            events.add(new FoodChangeEvent(stableId(extension.getTransitionId(), FoodChangeEvent.Kind.SLOWED, extension.getStepOrder()),
                    FoodChangeEvent.Kind.SLOWED, extension.getCreatedAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate(),
                    extension.getTransitionId()));
        }
        for (FoodTransitionPause pause : this.pauseRepository.findAllByTransitionIdIn(ids)) {
            events.add(new FoodChangeEvent(stableId(pause.getTransitionId(), FoodChangeEvent.Kind.PAUSED, pause.getId().intValue()),
                    FoodChangeEvent.Kind.PAUSED, pause.getPausedOn(), pause.getTransitionId()));
        }
        events.sort(Comparator.comparing(FoodChangeEvent::date).thenComparing(FoodChangeEvent::id).reversed());
        return events;
    }

    private static FoodChangeEvent event(FoodTransition transition, FoodChangeEvent.Kind kind, LocalDate date, int index) {
        return new FoodChangeEvent(stableId(transition.getId(), kind, index), kind, date, transition.getId());
    }

    private static UUID stableId(UUID transitionId, FoodChangeEvent.Kind kind, int index) {
        return UUID.nameUUIDFromBytes((transitionId + "|" + kind + "|" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private DayPortions dayPortions(UUID petId, LocalDate date, int mealsPerDay, Optional<ChangeView> change) {
        if (change.isPresent()) {
            Optional<DayPortions> inChange = this.portionsInChange(change.get(), date, mealsPerDay);
            if (inChange.isPresent()) {
                return inChange.get();
            }
        }
        List<PetFood> mains = this.mainFoodsOn(petId, date);
        if (mains.isEmpty()) {
            return new DayPortions(this.notices.askState(petId), List.of());
        }
        return this.usual(mains, mealsPerDay);
    }

    private Optional<DayPortions> portionsInChange(ChangeView view, LocalDate date, int mealsPerDay) {
        if (view.isFinishedAndUnseen()) {
            return date.isAfter(view.transition().getEndedOn())
                    ? Optional.of(this.usual(List.of(view.toFood()), mealsPerDay).withState(PortionState.FINISHED))
                    : Optional.empty();
        }
        ChangePhase phase = view.phaseOn(date);
        if (phase == ChangePhase.BEFORE_START) {
            return Optional.empty();
        }
        if (phase == ChangePhase.OVER) {
            return Optional.of(this.usual(List.of(view.toFood()), mealsPerDay).withState(PortionState.FINISHED));
        }
        DayMix mix = view.plan().mixOn(date).orElseThrow();
        boolean measured = mix.step().oldGrams() != null;
        List<MealPortion> portions = new ArrayList<>();
        portions.add(new MealPortion(view.fromFood().getId(), measured ? mix.step().oldGrams() : null));
        portions.add(new MealPortion(view.toFood().getId(), measured ? mix.step().newGrams() : null));
        if (measured) {
            portions.removeIf(portion -> portion.grams() == 0);
        }
        this.mainFoodsOn(view.fromFood().getPetId(), date).stream()
                .filter(food -> !food.getId().equals(view.fromFood().getId()) && !food.getId().equals(view.toFood().getId()))
                .forEach(steady -> portions.add(new MealPortion(steady.getId(),
                        MealPortionCalculator.perMealGrams(steady.getDailyGrams(), mealsPerDay))));
        PortionState state = view.transition().getStatus() == TransitionStatus.PAUSED ? PortionState.PAUSED
                : measured ? PortionState.MIX : PortionState.MIX_PARTS;
        return Optional.of(new DayPortions(state, portions));
    }

    private DayPortions usual(List<PetFood> mains, int mealsPerDay) {
        MealPortionCalculator.Result result = this.calculator.calculate(
                mains.stream().map(food -> new MealPortionCalculator.FoodShare(food.getId(), food.getDailyGrams())).toList(), mealsPerDay);
        return new DayPortions(result.state(), result.portions());
    }

    private List<PetFood> mainFoodsOn(UUID petId, LocalDate date) {
        return this.foodRepository.findGivenOn(petId, date, PetFoodStatus.ENTERED_IN_ERROR).stream()
                .filter(food -> food.getKind() == FoodKind.MAIN)
                .toList();
    }
}
