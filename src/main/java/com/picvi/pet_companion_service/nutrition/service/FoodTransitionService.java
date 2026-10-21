package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.exception.nutrition.AllergyMatchRequired;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeAlreadyActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeNotActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeNotFound;
import com.picvi.pet_companion_service.exception.nutrition.FoodProductNotFound;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodChange;
import com.picvi.pet_companion_service.exception.nutrition.PetFoodNotFound;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.CancelKeep;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.TransitionAction;
import com.picvi.pet_companion_service.nutrition.domain.TransitionEndReason;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.Plan;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanInput;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.dto.DietResponse;
import com.picvi.pet_companion_service.nutrition.dto.FoodChangeActionRequest;
import com.picvi.pet_companion_service.nutrition.dto.FoodChangePreviewResponse;
import com.picvi.pet_companion_service.nutrition.dto.IngredientDto;
import com.picvi.pet_companion_service.nutrition.dto.PlanStepDto;
import com.picvi.pet_companion_service.nutrition.dto.StartFoodChangeRequest;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.rhythm.api.RhythmApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class FoodTransitionService {
    static final int EARLY_DAYS = 14;
    static final int MAX_START_AHEAD_DAYS = 7;
    private static final List<TransitionStatus> OPEN = List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED);

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final RhythmApi rhythmApi;
    private final DietService dietService;
    private final FoodChangeReader changeReader;
    private final FoodChangeRecorder changeRecorder;
    private final FoodChangeActions actions;
    private final TummyReactionService tummyReactions;
    private final TransitionPlanCalculator calculator;
    private final FoodTransitionRepository transitionRepository;
    private final PetFoodRepository foodRepository;
    private final FoodProductRepository productRepository;
    private final FoodProductIngredientRepository ingredientRepository;
    private final ConditionApi conditionApi;
    private final AllergyMatcher allergyMatcher;
    private final Clock clock;

    /** What the change would look like, before it starts. */
    @Transactional(readOnly = true)
    public FoodChangePreviewResponse preview(UUID ownerId, UUID petId, UUID productId, UUID fromFoodId, TransitionPace pace,
                                             LocalDate startOn, Integer dailyGrams, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        this.requireNoOpenChange(petId);
        FoodProduct product = this.visibleProduct(ownerId, productId);
        PetFood from = this.resolveFrom(petId, fromFoodId, date);
        LocalDate start = this.startDate(pace, startOn, date);

        int mealsPerDay = this.rhythmApi.mealsPerDay(petId, date);
        Integer total = from.getDailyGrams() != null ? from.getDailyGrams() : dailyGrams;
        Plan plan = this.calculator.plan(new PlanInput(this.changeReader.paceSteps(pace.name()), start, List.of(), Map.of(),
                MealPortionCalculator.perMealGrams(total, mealsPerDay)), date);
        boolean early = ChronoUnit.DAYS.between(profile.createdOn(), start) < EARLY_DAYS;
        return new FoodChangePreviewResponse(pace, start, plan.lastDay(), plan.totalDays(), early,
                plan.steps().stream().map(PlanStepDto::of).toList(), this.matches(petId, product, date));
    }

    public DietResponse start(UUID ownerId, UUID petId, StartFoodChangeRequest request) {
        this.clientDateValidator.validate(request.date());
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        LocalDate date = request.date();
        if (request.clientId() != null && this.transitionRepository.findByPetIdAndClientId(petId, request.clientId()).isPresent()) {
            return this.dietService.dietFor(profile, date);
        }

        this.changeReader.closeIfFinished(petId, date, this.rhythmApi.mealsPerDay(petId, date));
        this.requireNoOpenChange(petId);
        FoodProduct product = this.visibleProduct(ownerId, request.productId());
        List<AllergyMatch> matches = this.matches(petId, product, date);
        if (!matches.isEmpty() && !Boolean.TRUE.equals(request.confirmAllergy())) {
            throw new AllergyMatchRequired(matches);
        }
        PetFood from = this.resolveFrom(petId, request.fromFoodId(), date);
        LocalDate startOn = this.startDate(request.pace(), request.startOn(), date);
        Integer dailyGrams = request.dailyGrams() != null ? request.dailyGrams() : from.getDailyGrams();
        if (dailyGrams != null && (dailyGrams < 1 || dailyGrams > 10000)) {
            throw new InvalidFoodChange("dailyGrams must be between 1 and 10000");
        }
        UUID clientId = request.clientId() == null ? UUID.randomUUID() : request.clientId();

        PetFood to = this.foodRepository.save(new PetFood(petId, UUID.randomUUID(), product, FoodKind.MAIN, dailyGrams, false,
                request.origin() == null ? FoodOrigin.OWNER : request.origin(), startOn, ownerId, this.clock.instant()));
        if (request.pace() == TransitionPace.IMMEDIATE) {
            this.changeRecorder.recordReplacement(petId, clientId, from, to, startOn);
        } else {
            this.transitionRepository.save(FoodTransition.planned(petId, clientId, from.getId(), to.getId(), request.pace(), startOn,
                    this.clock.instant()));
        }
        this.transitionRepository.flush();
        this.foodRepository.flush();
        return this.dietService.dietFor(profile, date);
    }

    public DietResponse act(UUID ownerId, UUID petId, UUID transitionId, FoodChangeActionRequest request) {
        this.clientDateValidator.validate(request.date());
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        LocalDate date = request.date();
        this.changeReader.closeIfFinished(petId, date, this.rhythmApi.mealsPerDay(petId, date));
        FoodTransition transition = this.transitionRepository.findByIdAndPetId(transitionId, petId).orElseThrow(FoodChangeNotFound::new);
        if (!transition.isOpen()) {
            throw new FoodChangeNotActive();
        }

        switch (request.action()) {
            case PAUSE -> this.actions.pause(transition, date);
            case RESUME -> this.actions.resume(transition, date);
            case SLOW_DOWN -> this.actions.slowDown(transition, date, null);
            case UNDO_EXTENSION -> this.actions.undoExtension(transition);
            case GO_BACK -> this.goBack(transition, TransitionEndReason.WENT_BACK, CancelKeep.OLD, date);
            case CANCEL -> this.goBack(transition, TransitionEndReason.CANCELLED,
                    request.keep() == null ? CancelKeep.OLD : request.keep(), date);
        }
        this.transitionRepository.saveAndFlush(transition);
        this.foodRepository.flush();
        return this.dietService.dietFor(profile, date);
    }

    /** The owner has seen the "all done" message. */
    public DietResponse markSeen(UUID ownerId, UUID petId, UUID transitionId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        FoodTransition transition = this.transitionRepository.findByIdAndPetId(transitionId, petId).orElseThrow(FoodChangeNotFound::new);
        transition.markFinishedSeen(OffsetDateTime.now(this.clock));
        this.tummyReactions.reactionOn(transition, date, this.rhythmApi.mealsPerDay(petId, date))
                .ifPresent(reaction -> transition.markReactionSeen(reaction.logId()));
        this.transitionRepository.saveAndFlush(transition);
        return this.dietService.dietFor(profile, date);
    }

    /** Ends the change and keeps one food: going back keeps the old one; cancelling lets the owner choose. */
    private void goBack(FoodTransition transition, TransitionEndReason reason, CancelKeep keep, LocalDate date) {
        PetFood from = this.foodRepository.findById(transition.getFromFoodId()).orElseThrow();
        PetFood to = this.foodRepository.findById(transition.getToFoodId()).orElseThrow();
        (keep == CancelKeep.NEW ? from : to).endOn(date);
        this.actions.endOpenPause(transition, date);
        transition.cancel(reason, date);
    }

    private void requireNoOpenChange(UUID petId) {
        if (this.transitionRepository.findOpen(petId, OPEN).isPresent()) {
            throw new FoodChangeAlreadyActive();
        }
    }

    private PetFood resolveFrom(UUID petId, UUID fromFoodId, LocalDate date) {
        List<PetFood> mains = this.foodRepository.findGivenOn(petId, date, PetFoodStatus.ENTERED_IN_ERROR).stream()
                .filter(food -> food.getKind() == FoodKind.MAIN)
                .toList();
        if (fromFoodId != null) {
            return mains.stream().filter(food -> food.getId().equals(fromFoodId)).findFirst().orElseThrow(PetFoodNotFound::new);
        }
        if (mains.isEmpty()) {
            throw new InvalidFoodChange("There is no main food to change from");
        }
        if (mains.size() > 1) {
            throw new InvalidFoodChange("Two main foods are being given: say which one to change (fromFoodId)");
        }
        return mains.get(0);
    }

    private LocalDate startDate(TransitionPace pace, LocalDate startOn, LocalDate date) {
        if (pace == TransitionPace.IMMEDIATE || startOn == null) {
            return date;
        }
        if (startOn.isBefore(date) || startOn.isAfter(date.plusDays(MAX_START_AHEAD_DAYS))) {
            throw new InvalidFoodChange("startOn must be between today and " + MAX_START_AHEAD_DAYS + " days from now");
        }
        return startOn;
    }

    private List<AllergyMatch> matches(UUID petId, FoodProduct product, LocalDate date) {
        List<AllergyMatcher.Ingredient> ingredients = this.ingredientRepository.findAllByProductIdIn(List.of(product.getId())).stream()
                .map(IngredientDto::of)
                .map(i -> new AllergyMatcher.Ingredient(i.code(), i.hydrolysed()))
                .toList();
        return this.allergyMatcher.match(ingredients, this.conditionApi.allergens(petId, date));
    }

    private FoodProduct visibleProduct(UUID ownerId, UUID productId) {
        return this.productRepository.findByIdAndStatus(productId, ProductStatus.PUBLIC)
                .or(() -> this.productRepository.findByIdAndStatusAndCreatedBy(productId, ProductStatus.OWNER_ADDED, ownerId))
                .orElseThrow(FoodProductNotFound::new);
    }
}
