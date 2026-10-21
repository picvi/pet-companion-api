package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeAlreadyActive;
import com.picvi.pet_companion_service.exception.nutrition.FoodChangeNotActive;
import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodChange;
import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher;
import com.picvi.pet_companion_service.nutrition.domain.CancelKeep;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.domain.TransitionAction;
import com.picvi.pet_companion_service.nutrition.domain.TransitionEndReason;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.dto.FoodChangeActionRequest;
import com.picvi.pet_companion_service.nutrition.dto.StartFoodChangeRequest;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductIngredientRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
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
class FoodTransitionServiceTest {
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
    private DietService dietService;
    @Mock
    private FoodChangeReader changeReader;
    @Mock
    private FoodChangeRecorder changeRecorder;
    @Mock
    private FoodTransitionRepository transitionRepository;
    @Mock
    private FoodChangeActions actions;
    @Mock
    private TummyReactionService tummyReactions;
    @Mock
    private PetFoodRepository foodRepository;
    @Mock
    private FoodProductRepository productRepository;
    @Mock
    private FoodProductIngredientRepository ingredientRepository;
    @Mock
    private ConditionApi conditionApi;

    private FoodTransitionService service;
    private FoodProduct product;
    private PetFood oldFood;
    private PetFood newFood;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new FoodTransitionService(this.clientDateValidator, this.petService, this.rhythmApi, this.dietService,
                this.changeReader, this.changeRecorder, this.actions, this.tummyReactions, new TransitionPlanCalculator(),
                this.transitionRepository, this.foodRepository, this.productRepository,
                this.ingredientRepository, this.conditionApi, new AllergyMatcher(), clock);
        this.product = FoodProduct.ownerAdded(OWNER, "Farmina", "Puppy", null, FoodForm.DRY, FoodKind.MAIN, LifeStage.PUPPY,
                Completeness.YES, ProteinStatus.KNOWN, "farmina puppy", Instant.now());
        ReflectionTestUtils.setField(this.product, "id", UUID.randomUUID());
        this.oldFood = this.food(120);
        this.newFood = this.food(120);
        lenient().when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(new PetProfile(PET, "Lumi", LocalDate.of(2026, 8, 6),
                SizeCategory.MEDIUM, TODAY.minusDays(30), VaccinationStatus.UNKNOWN, null, 0));
        lenient().when(this.rhythmApi.mealsPerDay(PET, TODAY)).thenReturn(4);
        lenient().when(this.productRepository.findByIdAndStatus(this.product.getId(), ProductStatus.PUBLIC)).thenReturn(Optional.empty());
        lenient().when(this.productRepository.findByIdAndStatusAndCreatedBy(this.product.getId(), ProductStatus.OWNER_ADDED, OWNER))
                .thenReturn(Optional.of(this.product));
        lenient().when(this.foodRepository.findById(this.oldFood.getId())).thenReturn(Optional.of(this.oldFood));
        lenient().when(this.foodRepository.findById(this.newFood.getId())).thenReturn(Optional.of(this.newFood));
        lenient().when(this.foodRepository.save(any(PetFood.class))).thenAnswer(call -> {
            PetFood saved = call.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
    }

    private PetFood food(int grams) {
        PetFood food = new PetFood(PET, UUID.randomUUID(), this.product, FoodKind.MAIN, grams, false, FoodOrigin.OWNER,
                TODAY.minusDays(5), OWNER, Instant.now());
        ReflectionTestUtils.setField(food, "id", UUID.randomUUID());
        return food;
    }

    private void givenMainFoods(PetFood... foods) {
        when(this.foodRepository.findGivenOn(PET, TODAY, PetFoodStatus.ENTERED_IN_ERROR)).thenReturn(List.of(foods));
    }

    private FoodTransition runningTransition() {
        FoodTransition transition = FoodTransition.planned(PET, UUID.randomUUID(), this.oldFood.getId(), this.newFood.getId(),
                TransitionPace.STANDARD, TODAY, Instant.now());
        ReflectionTestUtils.setField(transition, "id", UUID.randomUUID());
        lenient().when(this.transitionRepository.findByIdAndPetId(transition.getId(), PET)).thenReturn(Optional.of(transition));
        return transition;
    }

    private StartFoodChangeRequest start(TransitionPace pace) {
        return new StartFoodChangeRequest(UUID.randomUUID(), this.product.getId(), null, pace, null, null, null, null, TODAY);
    }

    @Test
    void startsAPlannedChangeWithTheNewFoodAsAMainFood() {
        this.givenMainFoods(this.oldFood);

        this.service.start(OWNER, PET, this.start(TransitionPace.STANDARD));

        ArgumentCaptor<PetFood> newFoodCaptor = ArgumentCaptor.forClass(PetFood.class);
        verify(this.foodRepository).save(newFoodCaptor.capture());
        assertThat(newFoodCaptor.getValue().getKind()).isEqualTo(FoodKind.MAIN);
        assertThat(newFoodCaptor.getValue().getDailyGrams()).isEqualTo(120);
        assertThat(newFoodCaptor.getValue().getStartedOn()).isEqualTo(TODAY);
        ArgumentCaptor<FoodTransition> transition = ArgumentCaptor.forClass(FoodTransition.class);
        verify(this.transitionRepository).save(transition.capture());
        assertThat(transition.getValue().getFromFoodId()).isEqualTo(this.oldFood.getId());
        assertThat(transition.getValue().getStatus()).isEqualTo(TransitionStatus.ACTIVE);
    }

    @Test
    void anAllAtOnceSwitchIsRecordedAsAReplacement() {
        this.givenMainFoods(this.oldFood);

        this.service.start(OWNER, PET, this.start(TransitionPace.IMMEDIATE));

        verify(this.changeRecorder).recordReplacement(eq(PET), any(), eq(this.oldFood), any(PetFood.class), eq(TODAY));
        verify(this.transitionRepository, never()).save(any());
    }

    @Test
    void aSecondChangeCannotStartWhileOneIsRunning() {
        FoodTransition running = this.runningTransition();
        when(this.transitionRepository.findOpen(eq(PET), any())).thenReturn(Optional.of(running));

        assertThatThrownBy(() -> this.service.start(OWNER, PET, this.start(TransitionPace.STANDARD)))
                .isInstanceOf(FoodChangeAlreadyActive.class);
    }

    @Test
    void thereMustBeAMainFoodToChangeFrom() {
        this.givenMainFoods();

        assertThatThrownBy(() -> this.service.start(OWNER, PET, this.start(TransitionPace.STANDARD))).isInstanceOf(InvalidFoodChange.class);
    }

    @Test
    void withTwoMainFoodsTheOwnerMustSayWhichToChange() {
        this.givenMainFoods(this.oldFood, this.food(100));

        assertThatThrownBy(() -> this.service.start(OWNER, PET, this.start(TransitionPace.STANDARD))).isInstanceOf(InvalidFoodChange.class);
    }

    @Test
    void startCannotBeMoreThanAWeekAhead() {
        this.givenMainFoods(this.oldFood);

        assertThatThrownBy(() -> this.service.start(OWNER, PET, new StartFoodChangeRequest(null, this.product.getId(), null,
                TransitionPace.STANDARD, TODAY.plusDays(8), null, null, null, TODAY))).isInstanceOf(InvalidFoodChange.class);
    }

    @Test
    void retryingAStartWithTheSameClientIdChangesNothing() {
        UUID clientId = UUID.randomUUID();
        FoodTransition running = this.runningTransition();
        when(this.transitionRepository.findByPetIdAndClientId(PET, clientId)).thenReturn(Optional.of(running));

        this.service.start(OWNER, PET, new StartFoodChangeRequest(clientId, this.product.getId(), null, TransitionPace.STANDARD,
                null, null, null, null, TODAY));

        verify(this.foodRepository, never()).save(any());
    }

    @Test
    void goingBackEndsTheNewFoodAndKeepsTheOldOne() {
        FoodTransition transition = this.runningTransition();

        this.service.act(OWNER, PET, transition.getId(), new FoodChangeActionRequest(TransitionAction.GO_BACK, null, TODAY));

        assertThat(transition.getStatus()).isEqualTo(TransitionStatus.CANCELLED);
        assertThat(transition.getEndReason()).isEqualTo(TransitionEndReason.WENT_BACK);
        assertThat(this.newFood.getStatus()).isEqualTo(PetFoodStatus.ENDED);
        assertThat(this.oldFood.getStatus()).isEqualTo(PetFoodStatus.ACTIVE);
    }

    @Test
    void cancellingCanKeepTheNewFood() {
        FoodTransition transition = this.runningTransition();

        this.service.act(OWNER, PET, transition.getId(), new FoodChangeActionRequest(TransitionAction.CANCEL, CancelKeep.NEW, TODAY));

        assertThat(transition.getEndReason()).isEqualTo(TransitionEndReason.CANCELLED);
        assertThat(this.oldFood.getStatus()).isEqualTo(PetFoodStatus.ENDED);
        assertThat(this.newFood.getStatus()).isEqualTo(PetFoodStatus.ACTIVE);
    }

    @Test
    void pausingAndResumingAreDelegatedToTheActions() {
        FoodTransition transition = this.runningTransition();

        this.service.act(OWNER, PET, transition.getId(), new FoodChangeActionRequest(TransitionAction.PAUSE, null, TODAY));
        this.service.act(OWNER, PET, transition.getId(), new FoodChangeActionRequest(TransitionAction.RESUME, null, TODAY.plusDays(1)));

        verify(this.actions).pause(transition, TODAY);
        verify(this.actions).resume(transition, TODAY.plusDays(1));
    }

    @Test
    void anActionOnAFinishedChangeIsRefused() {
        FoodTransition transition = this.runningTransition();
        transition.finish(TODAY.minusDays(1));

        assertThatThrownBy(() -> this.service.act(OWNER, PET, transition.getId(),
                new FoodChangeActionRequest(TransitionAction.PAUSE, null, TODAY))).isInstanceOf(FoodChangeNotActive.class);
    }
}
