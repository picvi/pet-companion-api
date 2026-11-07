package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.exception.nutrition.AllergyMatchRequired;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeAlreadyActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodProductNotFound;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodChange;
import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;
import com.picvi.pet_companion_service.exception.nutrition.MainFoodLimit;
import com.picvi.pet_companion_service.exception.nutrition.PetFoodNotFound;
import com.picvi.pet_companion_service.exception.nutrition.SecondMainFood;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.nutrition.api.MealPlan;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.nutrition.domain.AddFoodMode;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.ChangeView;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.NoticeCode;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodRules;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.PortionState;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinExposureCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.dto.AddPetFoodRequest;
import com.picvi.pet_companion_service.nutrition.dto.DietNoticesDto;
import com.picvi.pet_companion_service.nutrition.dto.DietResponse;
import com.picvi.pet_companion_service.nutrition.dto.FoodChangeDto;
import com.picvi.pet_companion_service.nutrition.dto.FoodProductDto;
import com.picvi.pet_companion_service.nutrition.dto.IngredientDto;
import com.picvi.pet_companion_service.nutrition.dto.PetFoodDto;
import com.picvi.pet_companion_service.nutrition.dto.ReactionDto;
import com.picvi.pet_companion_service.nutrition.dto.ProteinExposureDto;
import com.picvi.pet_companion_service.nutrition.dto.UpdatePetFoodRequest;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.ExposureReader;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetDietLock;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DietService {
    static final int KEEP_FOOD_TIP_DAYS = 14;
    static final int PROTEINS_WINDOW_DAYS = 56;
    static final int MAX_MAIN_FOODS = 2;

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final PetDietLock dietLock;
    private final RhythmApi rhythmApi;
    private final NutritionApi nutritionApi;
    private final PetFoodRepository foodRepository;
    private final FoodProductRepository productRepository;
    private final FoodProductIngredientRepository ingredientRepository;
    private final DietNotices notices;
    private final PetFoodRules rules;
    private final ConditionApi conditionApi;
    private final AllergyMatcher allergyMatcher;
    private final ExposureReader exposureReader;
    private final ProteinExposureCalculator exposureCalculator;
    private final FoodChangeReader changeReader;
    private final FoodChangeRecorder changeRecorder;
    private final FoodTransitionRepository transitionRepository;
    private final TummyReactionService tummyReactions;
    private final Clock clock;

    public DietResponse getDiet(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        return this.dietFor(this.petService.getOwnedPetProfile(ownerId, petId), date);
    }

    public DietResponse addFood(UUID ownerId, UUID petId, AddPetFoodRequest request) {
        this.clientDateValidator.validate(request.date());
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        this.dietLock.acquire(petId);
        if (request.clientId() != null && this.foodRepository.findByPetIdAndClientId(petId, request.clientId()).isPresent()) {
            return this.dietFor(profile, request.date());
        }

        FoodProduct product = this.visibleProduct(ownerId, request.productId());
        LocalDate date = request.date();
        LocalDate startedOn = request.startedOn() == null ? date : request.startedOn();
        boolean forTraining = Boolean.TRUE.equals(request.forTraining());
        this.rules.validate(request.kind(), request.dailyGrams(), forTraining, startedOn, date, profile.birthDate());

        List<AllergyMatch> matches = this.matchesFor(petId, product, date);
        if (!matches.isEmpty() && !Boolean.TRUE.equals(request.confirmAllergy())) {
            throw new AllergyMatchRequired(matches);
        }

        List<PetFood> replaced = List.of();
        if (request.kind() == FoodKind.MAIN) {
            List<PetFood> mains = this.mainFoodsGivenOn(petId, date);
            if (mains.stream().anyMatch(main -> main.getProduct().getId().equals(product.getId()))) {
                throw new InvalidPetFood("This food is already one of her main foods");
            }
            if (!mains.isEmpty()) {
                if (this.transitionRepository.findOpen(petId, List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED)).isPresent()) {
                    throw new FoodChangeAlreadyActive();
                }
                if (mains.size() >= MAX_MAIN_FOODS && request.mode() != AddFoodMode.REPLACE) {
                    throw new MainFoodLimit();
                }
                if (request.mode() == null) {
                    throw new SecondMainFood();
                }
                if (request.mode() == AddFoodMode.REPLACE) {
                    replaced = mains;
                    startedOn = date;
                }
            }
        }

        UUID clientId = request.clientId() == null ? UUID.randomUUID() : request.clientId();
        PetFood saved = this.foodRepository.save(new PetFood(petId, clientId, product,
                request.kind(), request.dailyGrams(), forTraining,
                startedOn, ownerId, this.clock.instant()));
        if (!replaced.isEmpty()) {
            replaced.stream().skip(1).forEach(extra -> extra.endOn(date));
            this.changeRecorder.recordReplacement(petId, clientId, replaced.get(0), saved, date);
        }
        this.foodRepository.flush();
        return this.dietFor(profile, date);
    }

    public DietResponse updateFood(UUID ownerId, UUID petId, UUID foodId, long expectedVersion, UpdatePetFoodRequest request) {
        this.clientDateValidator.validate(request.date());
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        PetFood food = this.ownedFood(petId, foodId);
        if (food.getVersion() != expectedVersion) {
            throw new VersionConflict();
        }
        boolean forTraining = Boolean.TRUE.equals(request.forTraining());
        this.rules.validate(food.getKind(), request.dailyGrams(), forTraining, null, request.date(), profile.birthDate());
        food.update(request.dailyGrams(), forTraining);
        this.foodRepository.saveAndFlush(food);
        return this.dietFor(profile, request.date());
    }

    /** The dog stopped eating it: from the given day it is no longer given. */
    public DietResponse endFood(UUID ownerId, UUID petId, UUID foodId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        this.dietLock.acquire(petId);
        PetFood food = this.ownedFood(petId, foodId);
        this.requireNotInRunningChange(petId, food);
        if (food.getStatus() == PetFoodStatus.ACTIVE) {
            food.endOn(date);
            this.foodRepository.saveAndFlush(food);
        }
        return this.dietFor(profile, date);
    }

    /** "Added by mistake": hidden everywhere, kept for audit. */
    public DietResponse removeFood(UUID ownerId, UUID petId, UUID foodId, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        this.dietLock.acquire(petId);
        PetFood food = this.ownedFood(petId, foodId);
        this.requireNotInRunningChange(petId, food);
        food.markEnteredInError();
        this.foodRepository.saveAndFlush(food);
        return this.dietFor(profile, date);
    }

    public DietResponse dismissNotice(UUID ownerId, UUID petId, NoticeCode code, LocalDate date) {
        this.clientDateValidator.validate(date);
        PetProfile profile = this.petService.getOwnedPetProfile(ownerId, petId);
        if (code == NoticeCode.FOOD_ASK_DISMISSED_AGAIN) {
            throw new InvalidPetFood("Send FOOD_ASK_DISMISSED; the server records the second dismissal itself");
        }
        this.notices.dismiss(petId, code, date);
        return this.dietFor(profile, date);
    }

    /** The whole diet on a day. A change whose last day has passed is closed first (done on read). */
    public DietResponse dietFor(PetProfile profile, LocalDate date) {
        UUID petId = profile.id();
        int mealsPerDay = this.rhythmApi.mealsPerDay(petId, date);
        this.changeReader.closeIfFinished(petId, date, mealsPerDay);
        this.tummyReactions.apply(petId, date);
        List<PetFood> foods = this.foodRepository.findGivenOn(petId, date, PetFoodStatus.ENTERED_IN_ERROR);
        Map<UUID, List<IngredientDto>> ingredients = this.ingredientsByProduct(foods);
        List<IngredientCode> allergens = this.conditionApi.allergens(petId, date);
        List<PetFoodDto> dtos = foods.stream()
                .map(food -> {
                    List<IngredientDto> productIngredients = ingredients.getOrDefault(food.getProduct().getId(), List.of());
                    List<AllergyMatch> matches = this.allergyMatcher.match(this.asMatcherInput(productIngredients), allergens);
                    return PetFoodDto.of(food, FoodProductDto.of(food.getProduct(), productIngredients, matches), mealsPerDay);
                })
                .toList();
        MealPlan plan = this.nutritionApi.portionsFor(petId, date, mealsPerDay);

        List<UUID> unknownProtein = foods.stream()
                .filter(food -> food.getProduct().getProteinStatus() == ProteinStatus.UNKNOWN)
                .map(PetFood::getId)
                .toList();
        List<ProteinExposureDto> proteins = this.exposureCalculator.calculate(
                this.exposureReader.read(petId, date.minusDays(PROTEINS_WINDOW_DAYS), date));
        FoodChangeDto change = this.changeReader.current(petId, date, mealsPerDay).map(view -> FoodChangeDto.of(view, date, this.reactionOf(view, date, mealsPerDay))).orElse(null);
        boolean canAddMainFood = change == null && foods.stream().filter(food -> food.getKind() == FoodKind.MAIN).count() < MAX_MAIN_FOODS;
        return new DietResponse(date, mealsPerDay, this.rhythmApi.mealsRange(petId, date), dtos, canAddMainFood, plan.today(), plan.tomorrow(), change, proteins, unknownProtein,
                this.noticesFor(profile, date, foods, unknownProtein));
    }

    private DietNoticesDto noticesFor(PetProfile profile, LocalDate date, List<PetFood> foods, List<UUID> unknownProtein) {
        Set<NoticeCode> seen = this.notices.seen(profile.id());
        boolean hasMain = foods.stream().anyMatch(food -> food.getKind() == FoodKind.MAIN);
        boolean foodAsk = !hasMain && this.notices.askState(profile.id()) == PortionState.ASK;
        boolean recentlyHome = ChronoUnit.DAYS.between(profile.createdOn(), date) < KEEP_FOOD_TIP_DAYS;
        boolean keepFoodTip = recentlyHome && hasMain && !seen.contains(NoticeCode.KEEP_FOOD_TIP);
        boolean proteinAsk = !unknownProtein.isEmpty() && !seen.contains(NoticeCode.PROTEIN_ASK);
        boolean rawNote = !seen.contains(NoticeCode.RAW_NOTE) && foods.stream().anyMatch(food ->
                food.getProduct().getForm() == FoodForm.RAW || food.getProduct().getForm() == FoodForm.HOME_COOKED);
        boolean complementaryNote = !seen.contains(NoticeCode.COMPLEMENTARY_NOTE) && foods.stream().anyMatch(food ->
                food.getKind() == FoodKind.MAIN && food.getProduct().getComplete() == Completeness.NO);
        return new DietNoticesDto(foodAsk, keepFoodTip, proteinAsk, rawNote, complementaryNote);
    }

    private ReactionDto reactionOf(ChangeView view, LocalDate date, int mealsPerDay) {
        return this.tummyReactions.reactionOn(view.transition(), date, mealsPerDay)
                .map(reaction -> new ReactionDto(reaction.kind(), reaction.logId(),
                        this.tummyReactions.stepOf(view.transition(), date, mealsPerDay).orElse(0),
                        reaction.logId().equals(view.transition().getReactionSeenLogId())))
                .orElse(null);
    }

    private List<AllergyMatch> matchesFor(UUID petId, FoodProduct product, LocalDate date) {
        List<IngredientDto> ingredients = this.ingredientRepository.findAllByProductIdIn(List.of(product.getId())).stream()
                .map(IngredientDto::of)
                .toList();
        return this.allergyMatcher.match(this.asMatcherInput(ingredients), this.conditionApi.allergens(petId, date));
    }

    private List<AllergyMatcher.Ingredient> asMatcherInput(List<IngredientDto> ingredients) {
        return ingredients.stream().map(i -> new AllergyMatcher.Ingredient(i.code(), i.hydrolysed())).toList();
    }

    private Map<UUID, List<IngredientDto>> ingredientsByProduct(List<PetFood> foods) {
        List<UUID> productIds = foods.stream().map(food -> food.getProduct().getId()).distinct().toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        return this.ingredientRepository.findAllByProductIdIn(productIds).stream()
                .collect(Collectors.groupingBy(FoodProductIngredient::getProductId,
                        Collectors.mapping(IngredientDto::of, Collectors.toList())));
    }

    /** A food in a running change is ended by going back or cancelling the change, not on its own. */
    private void requireNotInRunningChange(UUID petId, PetFood food) {
        this.transitionRepository.findOpen(petId, List.of(TransitionStatus.ACTIVE, TransitionStatus.PAUSED))
                .filter(t -> t.getFromFoodId().equals(food.getId()) || t.getToFoodId().equals(food.getId()))
                .ifPresent(t -> {
                    throw new InvalidFoodChange("This food is part of a running food change. Go back or cancel the change first");
                });
    }

    private List<PetFood> mainFoodsGivenOn(UUID petId, LocalDate date) {
        return this.foodRepository.findGivenOn(petId, date, PetFoodStatus.ENTERED_IN_ERROR).stream()
                .filter(food -> food.getKind() == FoodKind.MAIN)
                .toList();
    }

    private PetFood ownedFood(UUID petId, UUID foodId) {
        return this.foodRepository.findByIdAndPetId(foodId, petId)
                .filter(food -> food.getStatus() != PetFoodStatus.ENTERED_IN_ERROR)
                .orElseThrow(PetFoodNotFound::new);
    }

    private FoodProduct visibleProduct(UUID ownerId, UUID productId) {
        return this.productRepository.findByIdAndStatus(productId, ProductStatus.PUBLIC)
                .or(() -> this.productRepository.findByIdAndStatusAndCreatedBy(productId, ProductStatus.OWNER_ADDED, ownerId))
                .orElseThrow(FoodProductNotFound::new);
    }
}
