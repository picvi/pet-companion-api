package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPaceRow;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPaceRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodChangeReaderTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate MON = LocalDate.of(2026, 10, 19);

    @Mock
    private FoodTransitionRepository transitionRepository;
    @Mock
    private FoodTransitionExtensionRepository extensionRepository;
    @Mock
    private FoodTransitionPauseRepository pauseRepository;
    @Mock
    private FoodTransitionPaceRepository paceRepository;
    @Mock
    private PetFoodRepository foodRepository;

    private FoodChangeReader reader;
    private PetFood oldFood;
    private PetFood newFood;
    private FoodTransition transition;

    @BeforeEach
    void setUp() {
        this.reader = new FoodChangeReader(this.transitionRepository, this.extensionRepository, this.pauseRepository,
                this.paceRepository, this.foodRepository, new TransitionPlanCalculator());
        FoodProduct product = FoodProduct.ownerAdded(UUID.randomUUID(), "B", "P", null, FoodForm.DRY, FoodKind.MAIN, LifeStage.ALL,
                Completeness.YES, ProteinStatus.KNOWN, "b p", Instant.now());
        this.oldFood = this.food(product);
        this.newFood = this.food(product);
        this.transition = FoodTransition.planned(PET, UUID.randomUUID(), this.oldFood.getId(), this.newFood.getId(),
                TransitionPace.STANDARD, MON, Instant.now());
        lenient().when(this.transitionRepository.findOpen(any(), any())).thenReturn(Optional.of(this.transition));
        lenient().when(this.foodRepository.findById(this.oldFood.getId())).thenReturn(Optional.of(this.oldFood));
        lenient().when(this.foodRepository.findById(this.newFood.getId())).thenReturn(Optional.of(this.newFood));
        lenient().when(this.extensionRepository.findAllByTransitionId(any())).thenReturn(List.of());
        lenient().when(this.pauseRepository.findAllByTransitionIdOrderByPausedOnAsc(any())).thenReturn(List.of());
        lenient().when(this.paceRepository.findAllByPaceAndStatusOrderByStepOrderAsc("STANDARD", "ACTIVE")).thenReturn(List.of(
                row(1, 2, 25), row(2, 2, 50), row(3, 2, 75), row(4, 1, 100)));
    }

    private PetFood food(FoodProduct product) {
        PetFood food = new PetFood(PET, UUID.randomUUID(), product, FoodKind.MAIN, 120, false, FoodOrigin.OWNER, MON,
                UUID.randomUUID(), Instant.now());
        ReflectionTestUtils.setField(food, "id", UUID.randomUUID());
        return food;
    }

    private static FoodTransitionPaceRow row(int order, int days, int percent) {
        FoodTransitionPaceRow row = new FoodTransitionPaceRow();
        ReflectionTestUtils.setField(row, "pace", "STANDARD");
        ReflectionTestUtils.setField(row, "stepOrder", order);
        ReflectionTestUtils.setField(row, "days", days);
        ReflectionTestUtils.setField(row, "newPercent", percent);
        ReflectionTestUtils.setField(row, "status", "ACTIVE");
        return row;
    }

    @Test
    void theLastDayItselfStillBelongsToThePlan() {
        this.reader.closeIfFinished(PET, MON.plusDays(6), 4);

        assertThat(this.transition.getStatus()).isEqualTo(TransitionStatus.ACTIVE);
        assertThat(this.oldFood.getEndedOn()).isNull();
    }

    @Test
    void theFirstReadAfterTheLastDayClosesTheChange() {
        this.reader.closeIfFinished(PET, MON.plusDays(7), 4);

        assertThat(this.transition.getStatus()).isEqualTo(TransitionStatus.DONE);
        assertThat(this.transition.getEndedOn()).isEqualTo(MON.plusDays(6));
        assertThat(this.oldFood.getStatus()).isEqualTo(PetFoodStatus.ENDED);
        assertThat(this.oldFood.getEndedOn()).isEqualTo(MON.plusDays(6));
        assertThat(this.oldFood.isGivenOn(MON.plusDays(5))).isTrue();
        assertThat(this.oldFood.isGivenOn(MON.plusDays(6))).isFalse();
        assertThat(this.newFood.getEndedOn()).isNull();
    }

    @Test
    void aPausedChangeIsNeverClosed() {
        this.transition.pause();

        this.reader.closeIfFinished(PET, MON.plusDays(30), 4);

        assertThat(this.transition.getStatus()).isEqualTo(TransitionStatus.PAUSED);
    }
}
