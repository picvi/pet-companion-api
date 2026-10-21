package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.nutrition.AllergyMatchRequired;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeAlreadyActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodProductNotFound;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodChange;
import com.picvi.pet_companion_service.exception.nutrition.PetFoodNotFound;
import com.picvi.pet_companion_service.exception.nutrition.SecondMainFood;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.nutrition.api.DayPortions;
import com.picvi.pet_companion_service.nutrition.api.MealPlan;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.nutrition.domain.AddFoodMode;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatchKind;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.ExposureRow;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import com.picvi.pet_companion_service.nutrition.domain.ProteinExposureCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodRules;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.PortionState;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.dto.AddPetFoodRequest;
import com.picvi.pet_companion_service.nutrition.dto.DietResponse;
import com.picvi.pet_companion_service.nutrition.dto.UpdatePetFoodRequest;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodProductIngredient;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.ExposureReader;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.rhythm.api.RhythmApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private RhythmApi rhythmApi;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private PetFoodRepository foodRepository;
    @Mock
    private FoodProductRepository productRepository;
    @Mock
    private FoodProductIngredientRepository ingredientRepository;
    @Mock
    private DietNotices notices;
    @Mock
    private ConditionApi conditionApi;
    @Mock
    private ExposureReader exposureReader;
    @Mock
    private FoodChangeReader changeReader;
    @Mock
    private FoodChangeRecorder changeRecorder;
    @Mock
    private FoodTransitionRepository transitionRepository;
    @Mock
    private TummyReactionService tummyReactions;

    private DietService service;
    private FoodProduct product;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new DietService(this.clientDateValidator, this.petService, this.rhythmApi, this.nutritionApi,
                this.foodRepository, this.productRepository, this.ingredientRepository, this.notices, new PetFoodRules(),
                this.conditionApi, new AllergyMatcher(), this.exposureReader, new ProteinExposureCalculator(), this.changeReader,
                this.changeRecorder, this.transitionRepository, this.tummyReactions, clock);
        this.product = FoodProduct.ownerAdded(OWNER, "Brit", "Puppy", null, FoodForm.DRY, FoodKind.MAIN, LifeStage.PUPPY,
                Completeness.YES, ProteinStatus.KNOWN, "brit puppy", Instant.now());
        ReflectionTestUtils.setField(this.product, "id", UUID.randomUUID());
        lenient().when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(new PetProfile(PET, "Lumi", LocalDate.of(2026, 8, 6),
                SizeCategory.MEDIUM, TODAY.minusDays(3), VaccinationStatus.UNKNOWN, null, 0));
        lenient().when(this.rhythmApi.mealsPerDay(PET, TODAY)).thenReturn(4);
        lenient().when(this.nutritionApi.portionsFor(PET, TODAY, 4)).thenReturn(
                new MealPlan(new DayPortions(PortionState.USUAL, List.of()), new DayPortions(PortionState.USUAL, List.of()), null, null));
        lenient().when(this.notices.seen(PET)).thenReturn(Set.of());
        lenient().when(this.notices.askState(PET)).thenReturn(PortionState.ASK);
    }

    private PetFood food(FoodKind kind, int grams) {
        PetFood food = new PetFood(PET, UUID.randomUUID(), this.product, kind, grams, false, FoodOrigin.BREEDER, TODAY.minusDays(2),
                OWNER, Instant.now());
        ReflectionTestUtils.setField(food, "id", UUID.randomUUID());
        return food;
    }

    private AddPetFoodRequest add(FoodKind kind, AddFoodMode mode) {
        return new AddPetFoodRequest(UUID.randomUUID(), this.product.getId(), kind, 100, null, null, mode, null, null, TODAY);
    }

    private void givenVisibleProduct() {
        when(this.productRepository.findByIdAndStatus(this.product.getId(), ProductStatus.PUBLIC)).thenReturn(Optional.empty());
        when(this.productRepository.findByIdAndStatusAndCreatedBy(this.product.getId(), ProductStatus.OWNER_ADDED, OWNER))
                .thenReturn(Optional.of(this.product));
    }

    private void givenGiven(PetFood... foods) {
        when(this.foodRepository.findGivenOn(eq(PET), eq(TODAY), eq(PetFoodStatus.ENTERED_IN_ERROR))).thenReturn(List.of(foods));
    }

    @Test
    void getDietShowsPerMealGramsAndTheFoodQuestionWhenThereIsNoMainFood() {
        this.givenGiven();

        DietResponse diet = this.service.getDiet(OWNER, PET, TODAY);

        assertThat(diet.mealsPerDay()).isEqualTo(4);
        assertThat(diet.foods()).isEmpty();
        assertThat(diet.notices().foodAsk()).isTrue();
    }

    @Test
    void getDietCalculatesTheAmountPerMeal() {
        this.givenGiven(this.food(FoodKind.MAIN, 120));

        DietResponse diet = this.service.getDiet(OWNER, PET, TODAY);

        assertThat(diet.foods()).singleElement().satisfies(food -> {
            assertThat(food.perMealGrams()).isEqualTo(30);
            assertThat(food.dailyGrams()).isEqualTo(120);
        });
        assertThat(diet.notices().foodAsk()).isFalse();
    }

    @Test
    void getDietListsFoodsWithAnUnknownProteinAndAsksOnce() {
        this.product = FoodProduct.ownerAdded(OWNER, null, "Mine", null, FoodForm.DRY, FoodKind.MAIN, LifeStage.ALL,
                Completeness.UNKNOWN, ProteinStatus.UNKNOWN, "mine", Instant.now());
        ReflectionTestUtils.setField(this.product, "id", UUID.randomUUID());
        PetFood food = this.food(FoodKind.MAIN, 100);
        this.givenGiven(food);

        DietResponse diet = this.service.getDiet(OWNER, PET, TODAY);

        assertThat(diet.unknownProteinFoodIds()).containsExactly(food.getId());
        assertThat(diet.notices().proteinAsk()).isTrue();
    }

    @Test
    void keepFoodTipShowsForTheBreedersFoodDuringTheFirstTwoWeeks() {
        this.givenGiven(this.food(FoodKind.MAIN, 100));

        assertThat(this.service.getDiet(OWNER, PET, TODAY).notices().keepFoodTip()).isTrue();
    }

    @Test
    void aSecondMainFoodNeedsAnAnswer() {
        this.givenVisibleProduct();
        this.givenGiven(this.food(FoodKind.MAIN, 100));

        assertThatThrownBy(() -> this.service.addFood(OWNER, PET, this.add(FoodKind.MAIN, null))).isInstanceOf(SecondMainFood.class);
        verify(this.foodRepository, never()).save(any());
    }

    @Test
    void feedBothKeepsTheFirstFood() {
        this.givenVisibleProduct();
        PetFood existing = this.food(FoodKind.MAIN, 100);
        this.givenGiven(existing);

        this.service.addFood(OWNER, PET, this.add(FoodKind.MAIN, AddFoodMode.FEED_BOTH));

        verify(this.foodRepository).save(any(PetFood.class));
        assertThat(existing.getStatus()).isEqualTo(PetFoodStatus.ACTIVE);
        assertThat(existing.getEndedOn()).isNull();
    }

    @Test
    void replaceRecordsAnAllAtOnceSwitchStartingToday() {
        this.givenVisibleProduct();
        PetFood existing = this.food(FoodKind.MAIN, 100);
        this.givenGiven(existing);
        when(this.foodRepository.save(any(PetFood.class))).thenAnswer(call -> call.getArgument(0));
        UUID clientId = UUID.randomUUID();

        this.service.addFood(OWNER, PET, new AddPetFoodRequest(clientId, this.product.getId(), FoodKind.MAIN, 100, null,
                null, AddFoodMode.REPLACE, TODAY.minusDays(5), null, TODAY));

        ArgumentCaptor<PetFood> saved = ArgumentCaptor.forClass(PetFood.class);
        verify(this.foodRepository).save(saved.capture());
        assertThat(saved.getValue().getStartedOn()).isEqualTo(TODAY);
        verify(this.changeRecorder).recordReplacement(PET, clientId, existing, saved.getValue(), TODAY);
    }

    @Test
    void feedingBothWhileAChangeRunsIsRefused() {
        this.givenVisibleProduct();
        this.givenGiven(this.food(FoodKind.MAIN, 100));
        when(this.transitionRepository.findOpen(eq(PET), any())).thenReturn(Optional.of(
                FoodTransition.planned(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TransitionPace.STANDARD, TODAY, Instant.now())));

        assertThatThrownBy(() -> this.service.addFood(OWNER, PET, this.add(FoodKind.MAIN, AddFoodMode.FEED_BOTH)))
                .isInstanceOf(FoodChangeAlreadyActive.class);
    }

    @Test
    void aFoodInARunningChangeCannotBeRemovedOnItsOwn() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        when(this.foodRepository.findByIdAndPetId(food.getId(), PET)).thenReturn(Optional.of(food));
        when(this.transitionRepository.findOpen(eq(PET), any())).thenReturn(Optional.of(
                FoodTransition.planned(PET, UUID.randomUUID(), food.getId(), UUID.randomUUID(), TransitionPace.STANDARD, TODAY, Instant.now())));

        assertThatThrownBy(() -> this.service.removeFood(OWNER, PET, food.getId(), TODAY)).isInstanceOf(InvalidFoodChange.class);
        assertThat(food.getStatus()).isEqualTo(PetFoodStatus.ACTIVE);
    }

    @Test
    void aTreatNeedsNoAnswerEvenWithAMainFood() {
        this.givenVisibleProduct();
        this.givenGiven(this.food(FoodKind.MAIN, 100));

        this.service.addFood(OWNER, PET, this.add(FoodKind.TREAT, null));

        verify(this.foodRepository).save(any(PetFood.class));
    }

    @Test
    void retryingWithTheSameClientIdAddsNothing() {
        UUID clientId = UUID.randomUUID();
        when(this.foodRepository.findByPetIdAndClientId(PET, clientId)).thenReturn(Optional.of(this.food(FoodKind.MAIN, 100)));
        this.givenGiven();

        this.service.addFood(OWNER, PET, new AddPetFoodRequest(clientId, this.product.getId(), FoodKind.MAIN, 100, null, null, null, null, null, TODAY));

        verify(this.foodRepository, never()).save(any());
    }

    @Test
    void anUnknownOrForeignProductIsNotFound() {
        when(this.productRepository.findByIdAndStatus(any(), any())).thenReturn(Optional.empty());
        when(this.productRepository.findByIdAndStatusAndCreatedBy(any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.addFood(OWNER, PET, this.add(FoodKind.MAIN, null))).isInstanceOf(FoodProductNotFound.class);
    }

    @Test
    void updateWithAStaleVersionIsRejected() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        when(this.foodRepository.findByIdAndPetId(food.getId(), PET)).thenReturn(Optional.of(food));

        assertThatThrownBy(() -> this.service.updateFood(OWNER, PET, food.getId(), 7, new UpdatePetFoodRequest(90, false, null, TODAY)))
                .isInstanceOf(VersionConflict.class);
        verify(this.foodRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateChangesTheAmount() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        when(this.foodRepository.findByIdAndPetId(food.getId(), PET)).thenReturn(Optional.of(food));
        this.givenGiven(food);

        this.service.updateFood(OWNER, PET, food.getId(), food.getVersion(), new UpdatePetFoodRequest(90, false, null, TODAY));

        assertThat(food.getDailyGrams()).isEqualTo(90);
        assertThat(food.getOrigin()).isEqualTo(FoodOrigin.BREEDER);
    }

    @Test
    void removeHidesTheFoodButKeepsIt() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        when(this.foodRepository.findByIdAndPetId(food.getId(), PET)).thenReturn(Optional.of(food));
        this.givenGiven();

        this.service.removeFood(OWNER, PET, food.getId(), TODAY);

        assertThat(food.getStatus()).isEqualTo(PetFoodStatus.ENTERED_IN_ERROR);
        verify(this.foodRepository, never()).delete(any());
    }

    @Test
    void endingAFoodSetsTheFirstDayItIsNoLongerGiven() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        when(this.foodRepository.findByIdAndPetId(food.getId(), PET)).thenReturn(Optional.of(food));
        this.givenGiven();

        this.service.endFood(OWNER, PET, food.getId(), TODAY);

        assertThat(food.getStatus()).isEqualTo(PetFoodStatus.ENDED);
        assertThat(food.getEndedOn()).isEqualTo(TODAY);
    }

    @Test
    void aRemovedFoodCannotBeChangedAgain() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        food.markEnteredInError();
        when(this.foodRepository.findByIdAndPetId(food.getId(), PET)).thenReturn(Optional.of(food));

        assertThatThrownBy(() -> this.service.endFood(OWNER, PET, food.getId(), TODAY)).isInstanceOf(PetFoodNotFound.class);
    }

    @Test
    void addingAFoodWithAnAllergenNeedsConfirmation() {
        this.givenVisibleProduct();
        when(this.conditionApi.allergens(PET, TODAY)).thenReturn(List.of(IngredientCode.CHICKEN));
        when(this.ingredientRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                new FoodProductIngredient(this.product.getId(), "CHICKEN", IngredientRole.MAIN, false)));

        assertThatThrownBy(() -> this.service.addFood(OWNER, PET, this.add(FoodKind.MAIN, null)))
                .isInstanceOfSatisfying(AllergyMatchRequired.class, e ->
                        assertThat(e.getMatches()).containsExactly(new AllergyMatch(IngredientCode.CHICKEN, AllergyMatchKind.CONTAINS)));
        verify(this.foodRepository, never()).save(any());
    }

    @Test
    void addingTheFoodAnywayWhenConfirmed() {
        this.givenVisibleProduct();
        this.givenGiven();
        when(this.conditionApi.allergens(PET, TODAY)).thenReturn(List.of(IngredientCode.CHICKEN));
        when(this.ingredientRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                new FoodProductIngredient(this.product.getId(), "CHICKEN", IngredientRole.MAIN, false)));

        this.service.addFood(OWNER, PET, new AddPetFoodRequest(UUID.randomUUID(), this.product.getId(), FoodKind.MAIN, 100, null, null,
                null, null, true, TODAY));

        verify(this.foodRepository).save(any(PetFood.class));
    }

    @Test
    void dietMarksEachFoodWithTheAllergensItTouches() {
        PetFood food = this.food(FoodKind.MAIN, 100);
        this.givenGiven(food);
        when(this.conditionApi.allergens(PET, TODAY)).thenReturn(List.of(IngredientCode.CHICKEN));
        when(this.ingredientRepository.findAllByProductIdIn(any())).thenReturn(List.of(
                new FoodProductIngredient(this.product.getId(), "POULTRY", IngredientRole.MAIN, false)));

        DietResponse diet = this.service.getDiet(OWNER, PET, TODAY);

        assertThat(diet.foods().get(0).allergyMatches()).containsExactly(new AllergyMatch(IngredientCode.CHICKEN, AllergyMatchKind.MAY_CONTAIN));
    }

    @Test
    void dietListsTheProteinsEatenOverTheLastEightWeeks() {
        this.givenGiven();
        when(this.exposureReader.read(PET, TODAY.minusDays(56), TODAY)).thenReturn(List.of(
                new ExposureRow(IngredientCode.CHICKEN, FoodKind.MAIN, false, LocalDate.of(2026, 10, 1), null)));

        DietResponse diet = this.service.getDiet(OWNER, PET, TODAY);

        assertThat(diet.proteins()).singleElement().satisfies(protein -> {
            assertThat(protein.code()).isEqualTo(IngredientCode.CHICKEN);
            assertThat(protein.firstOn()).isEqualTo(LocalDate.of(2026, 10, 1));
            assertThat(protein.lastOn()).isNull();
        });
    }
}
