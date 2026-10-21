package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PaceStep;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanInput;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionExtension;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPause;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import com.picvi.pet_companion_service.rhythm.api.RhythmApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodChangeActionsTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private RhythmApi rhythmApi;
    @Mock
    private FoodChangeReader changeReader;
    @Mock
    private PetFoodRepository foodRepository;
    @Mock
    private FoodTransitionExtensionRepository extensionRepository;
    @Mock
    private FoodTransitionPauseRepository pauseRepository;

    private FoodChangeActions actions;
    private FoodTransition transition;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.actions = new FoodChangeActions(this.rhythmApi, this.changeReader, this.foodRepository, this.extensionRepository,
                this.pauseRepository, clock);
        FoodProduct product = FoodProduct.ownerAdded(UUID.randomUUID(), "B", "P", null, FoodForm.DRY, FoodKind.MAIN, LifeStage.ALL,
                Completeness.YES, ProteinStatus.KNOWN, "b p", Instant.now());
        PetFood from = this.food(product);
        PetFood to = this.food(product);
        this.transition = FoodTransition.planned(PET, UUID.randomUUID(), from.getId(), to.getId(), TransitionPace.STANDARD, TODAY, Instant.now());
        ReflectionTestUtils.setField(this.transition, "id", UUID.randomUUID());
    }

    private PetFood food(FoodProduct product) {
        PetFood food = new PetFood(PET, UUID.randomUUID(), product, FoodKind.MAIN, 120, false, FoodOrigin.OWNER, TODAY, UUID.randomUUID(), Instant.now());
        ReflectionTestUtils.setField(food, "id", UUID.randomUUID());
        lenientFind(food);
        return food;
    }

    private void lenientFind(PetFood food) {
        org.mockito.Mockito.lenient().when(this.foodRepository.findById(food.getId())).thenReturn(Optional.of(food));
    }

    private void givenPlanOnStepOne() {
        when(this.rhythmApi.mealsPerDay(PET, TODAY)).thenReturn(4);
        when(this.changeReader.plan(eq(this.transition), any(), any(), eq(TODAY), eq(4))).thenReturn(new TransitionPlanCalculator().plan(
                new PlanInput(List.of(new PaceStep(1, 2, 25), new PaceStep(2, 2, 50)), TODAY, List.of(), Map.of(), 30), TODAY));
    }

    @Test
    void aStepIsSlowedDownOnce() {
        this.givenPlanOnStepOne();
        when(this.extensionRepository.findByTransitionIdAndStepOrder(this.transition.getId(), 1)).thenReturn(Optional.empty());

        assertThat(this.actions.slowDown(this.transition, TODAY, null)).isTrue();

        verify(this.extensionRepository).saveAndFlush(any(FoodTransitionExtension.class));
    }

    @Test
    void aSecondSlowDownOfTheSameStepChangesNothing() {
        this.givenPlanOnStepOne();
        when(this.extensionRepository.findByTransitionIdAndStepOrder(this.transition.getId(), 1)).thenReturn(Optional.of(
                new FoodTransitionExtension(this.transition.getId(), 1, 2, null, OffsetDateTime.now())));

        assertThat(this.actions.slowDown(this.transition, TODAY, null)).isFalse();
        assertThat(this.actions.slowDown(this.transition, TODAY, UUID.randomUUID())).isFalse();

        verify(this.extensionRepository, never()).saveAndFlush(any());
    }

    @Test
    void anAutomaticSlowDownDoesNotRevivateOneTheOwnerUndid() {
        this.givenPlanOnStepOne();
        FoodTransitionExtension undone = new FoodTransitionExtension(this.transition.getId(), 1, 2, UUID.randomUUID(), OffsetDateTime.now());
        undone.tombstone();
        when(this.extensionRepository.findByTransitionIdAndStepOrder(this.transition.getId(), 1)).thenReturn(Optional.of(undone));

        assertThat(this.actions.slowDown(this.transition, TODAY, UUID.randomUUID())).isFalse();
        assertThat(undone.getDays()).isZero();
    }

    @Test
    void theOwnersOwnRequestRevivesAnUndoneSlowDown() {
        this.givenPlanOnStepOne();
        FoodTransitionExtension undone = new FoodTransitionExtension(this.transition.getId(), 1, 2, UUID.randomUUID(), OffsetDateTime.now());
        undone.tombstone();
        when(this.extensionRepository.findByTransitionIdAndStepOrder(this.transition.getId(), 1)).thenReturn(Optional.of(undone));

        assertThat(this.actions.slowDown(this.transition, TODAY, null)).isTrue();
        assertThat(undone.getDays()).isEqualTo(FoodChangeActions.SLOW_DOWN_DAYS);
    }

    @Test
    void undoingAnAutomaticSlowDownKeepsAMarkerButAnOwnersIsDeleted() {
        FoodTransitionExtension automatic = new FoodTransitionExtension(this.transition.getId(), 1, 2, UUID.randomUUID(), OffsetDateTime.now());
        FoodTransitionExtension manual = new FoodTransitionExtension(this.transition.getId(), 2, 2, null, OffsetDateTime.now().plusMinutes(1));
        ReflectionTestUtils.setField(automatic, "id", 1L);
        ReflectionTestUtils.setField(manual, "id", 2L);
        when(this.extensionRepository.findAllByTransitionId(this.transition.getId())).thenReturn(List.of(automatic, manual));

        this.actions.undoExtension(this.transition);
        verify(this.extensionRepository).delete(manual);

        when(this.extensionRepository.findAllByTransitionId(this.transition.getId())).thenReturn(List.of(automatic));
        this.actions.undoExtension(this.transition);
        assertThat(automatic.isTombstone()).isTrue();
    }

    @Test
    void pausingStartsAnIntervalAndResumingEndsIt() {
        FoodTransitionPause[] stored = new FoodTransitionPause[1];
        when(this.pauseRepository.save(any(FoodTransitionPause.class))).thenAnswer(call -> {
            stored[0] = call.getArgument(0);
            return stored[0];
        });

        this.actions.pause(this.transition, TODAY);
        assertThat(this.transition.getStatus()).isEqualTo(TransitionStatus.PAUSED);
        this.actions.pause(this.transition, TODAY.plusDays(1));
        verify(this.pauseRepository).save(any());

        when(this.pauseRepository.findFirstByTransitionIdAndResumedOnIsNull(this.transition.getId())).thenReturn(Optional.of(stored[0]));
        this.actions.resume(this.transition, TODAY.plusDays(3));

        assertThat(this.transition.getStatus()).isEqualTo(TransitionStatus.ACTIVE);
        assertThat(stored[0].getResumedOn()).isEqualTo(TODAY.plusDays(3));
    }
}
