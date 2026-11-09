package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.nutrition.domain.ChangeMethod;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.ExposureRow;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinExposureCalculator;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;
import com.picvi.pet_companion_service.nutrition.repository.ExposureReader;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DietHistoryServiceTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 20);

    @Mock
    private PetFoodRepository foodRepository;
    @Mock
    private FoodTransitionRepository transitionRepository;
    @Mock
    private ExposureReader exposureReader;

    private DietHistoryService service;

    @BeforeEach
    void setUp() {
        this.service = new DietHistoryService(this.foodRepository, this.transitionRepository, this.exposureReader, new ProteinExposureCalculator());
    }

    private PetFood food(String name, FoodKind kind, LocalDate started, LocalDate ended) {
        FoodProduct product = FoodProduct.ownerAdded(UUID.randomUUID(), "Brand", name, null, FoodForm.DRY, kind, LifeStage.ALL,
                Completeness.YES, ProteinStatus.KNOWN, name, Instant.now());
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        PetFood food = new PetFood(PET, UUID.randomUUID(), product, kind, 100, false, started, UUID.randomUUID(), Instant.now());
        ReflectionTestUtils.setField(food, "id", UUID.randomUUID());
        if (ended != null) {
            food.endOn(ended);
        }
        return food;
    }

    @Test
    void aFoodSwitchedAllAtOnceOnThe9thShowsBothFoodsAndTheMethod() {
        PetFood before = this.food("Old", FoodKind.MAIN, LocalDate.of(2026, 9, 26), LocalDate.of(2026, 10, 9));
        PetFood now = this.food("New", FoodKind.MAIN, LocalDate.of(2026, 10, 9), null);
        when(this.foodRepository.findOverlapping(PET, FROM, TO, PetFoodStatus.ENTERED_IN_ERROR)).thenReturn(List.of(before, now));
        FoodTransition switched = FoodTransition.replacement(PET, UUID.randomUUID(), before.getId(), now.getId(), LocalDate.of(2026, 10, 9),
                Instant.now(), OffsetDateTime.now());
        when(this.transitionRepository.findAllByPetIdOrderByStartOnDesc(PET)).thenReturn(List.of(switched));

        DietHistoryResponse history = this.service.history(PET, FROM, TO);

        assertThat(history.mainFoods()).hasSize(2);
        assertThat(history.mainFoods().get(0).from()).isEqualTo(LocalDate.of(2026, 9, 26));
        assertThat(history.mainFoods().get(0).to()).isEqualTo(LocalDate.of(2026, 10, 8));
        assertThat(history.mainFoods().get(1).from()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(history.mainFoods().get(1).to()).isNull();
        assertThat(history.changes()).singleElement().satisfies(change -> {
            assertThat(change.method()).isEqualTo(ChangeMethod.ALL_AT_ONCE);
            assertThat(change.from()).isEqualTo(LocalDate.of(2026, 10, 9));
        });
    }

    @Test
    void treatsAndChewsAreListedApartFromMainFoods() {
        when(this.foodRepository.findOverlapping(PET, FROM, TO, PetFoodStatus.ENTERED_IN_ERROR)).thenReturn(List.of(
                this.food("Kibble", FoodKind.MAIN, LocalDate.of(2026, 9, 1), null), this.food("Chew", FoodKind.CHEW, LocalDate.of(2026, 10, 1), null),
                this.food("Treat", FoodKind.TREAT, LocalDate.of(2026, 10, 2), null)));
        when(this.transitionRepository.findAllByPetIdOrderByStartOnDesc(PET)).thenReturn(List.of());

        DietHistoryResponse history = this.service.history(PET, FROM, TO);

        assertThat(history.mainFoods()).hasSize(1);
        assertThat(history.treatsAndChews()).extracting(food -> food.productName()).containsExactly("Chew", "Treat");
    }

    @Test
    void plannedAndCancelledChangesAreToldApartAndOutsideTheRangeAreLeftOut() {
        FoodTransition planned = FoodTransition.planned(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TransitionPace.STANDARD,
                LocalDate.of(2026, 10, 1), Instant.now());
        planned.finish(LocalDate.of(2026, 10, 7));
        FoodTransition cancelled = FoodTransition.planned(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TransitionPace.SLOW,
                LocalDate.of(2026, 10, 10), Instant.now());
        cancelled.cancel(com.picvi.pet_companion_service.nutrition.domain.TransitionEndReason.WENT_BACK, LocalDate.of(2026, 10, 13));
        FoodTransition old = FoodTransition.planned(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TransitionPace.STANDARD,
                LocalDate.of(2026, 3, 1), Instant.now());
        old.finish(LocalDate.of(2026, 3, 7));
        when(this.foodRepository.findOverlapping(PET, FROM, TO, PetFoodStatus.ENTERED_IN_ERROR)).thenReturn(List.of());
        when(this.transitionRepository.findAllByPetIdOrderByStartOnDesc(PET)).thenReturn(List.of(cancelled, planned, old));

        DietHistoryResponse history = this.service.history(PET, FROM, TO);

        assertThat(history.changes()).extracting(change -> change.method()).containsExactly(ChangeMethod.PLANNED, ChangeMethod.CANCELLED);
    }

    @Test
    void proteinsEatenInThePeriodAreIncluded() {
        when(this.foodRepository.findOverlapping(PET, FROM, TO, PetFoodStatus.ENTERED_IN_ERROR)).thenReturn(List.of());
        when(this.transitionRepository.findAllByPetIdOrderByStartOnDesc(PET)).thenReturn(List.of());
        when(this.exposureReader.read(PET, FROM, TO)).thenReturn(List.of(
                new ExposureRow(IngredientCode.SALMON, FoodKind.TREAT, false, LocalDate.of(2026, 10, 9), null)));

        assertThat(this.service.history(PET, FROM, TO).proteins()).singleElement()
                .satisfies(protein -> assertThat(protein.code()).isEqualTo(IngredientCode.SALMON));
    }

    @Test
    void theRangeMustBeSensible() {
        assertThatThrownBy(() -> this.service.history(PET, TO, FROM)).isInstanceOf(InvalidDate.class);
        assertThatThrownBy(() -> this.service.history(PET, FROM.minusYears(2), TO)).isInstanceOf(InvalidDate.class);
    }
}
