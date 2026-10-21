package com.picvi.pet_companion_service.nutrition.api;

import com.picvi.pet_companion_service.nutrition.api.FoodChangeNow.State;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductChangeRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodProductRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.nutrition.repository.PetFoodRepository;
import com.picvi.pet_companion_service.nutrition.service.DietHistoryService;
import com.picvi.pet_companion_service.nutrition.service.DietNotices;
import com.picvi.pet_companion_service.nutrition.service.FoodChangeReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NutritionApiTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private PetFoodRepository foodRepository;
    @Mock
    private MealPortionCalculator calculator;
    @Mock
    private DietNotices notices;
    @Mock
    private FoodTransitionRepository transitionRepository;
    @Mock
    private FoodTransitionExtensionRepository extensionRepository;
    @Mock
    private FoodTransitionPauseRepository pauseRepository;
    @Mock
    private DietHistoryService historyService;
    @Mock
    private FoodProductRepository productRepository;
    @Mock
    private FoodProductChangeRepository productChangeRepository;

    private FoodChangeReader changeReader;
    private NutritionApi api;

    @BeforeEach
    void setUp() {
        this.changeReader = mock(FoodChangeReader.class, RETURNS_DEEP_STUBS);
        lenient().when(this.changeReader.view(any(), any(), anyInt()).plan().isOver(any())).thenReturn(false);
        this.api = new NutritionApi(this.foodRepository, this.calculator, this.notices, this.changeReader, this.transitionRepository,
                this.extensionRepository, this.pauseRepository, this.historyService,
                this.productRepository, this.productChangeRepository);
    }

    @Test
    void forgettingAUserClearsEveryCatalogueLink() {
        this.api.forgetUser(PET);

        verify(this.productRepository).clearCreatedBy(PET);
        verify(this.productRepository).clearReviewedBy(PET);
        verify(this.productChangeRepository).clearChangedBy(PET);
    }

    @Test
    void aRunningChangeIsRunning() {
        givenTransitions(planned(TODAY.minusDays(2)));

        assertThat(this.api.changeNow(PET, TODAY, 14)).contains(new FoodChangeNow(State.RUNNING, TODAY.minusDays(2)));
    }

    @Test
    void aPausedChangeIsPausedHoweverOldItIs() {
        FoodTransition transition = planned(TODAY.minusDays(30));
        transition.pause();
        givenTransitions(transition);

        assertThat(this.api.changeNow(PET, TODAY, 14)).contains(new FoodChangeNow(State.PAUSED, TODAY.minusDays(30)));
    }

    @Test
    void aChangeThatEndedTenDaysAgoIsStillThere() {
        FoodTransition transition = planned(TODAY.minusDays(16));
        transition.finish(TODAY.minusDays(10));
        givenTransitions(transition);

        assertThat(this.api.changeNow(PET, TODAY, 20)).contains(new FoodChangeNow(State.ENDED, TODAY.minusDays(16)));
    }

    @Test
    void aChangeThatStartedLongAgoAndEndedIsGone() {
        FoodTransition transition = planned(TODAY.minusDays(30));
        transition.finish(TODAY.minusDays(24));
        givenTransitions(transition);

        assertThat(this.api.changeNow(PET, TODAY, 14)).isEmpty();
    }

    @Test
    void aChangeStartingTomorrowIsNotYet() {
        givenTransitions(planned(TODAY.plusDays(1)));

        assertThat(this.api.changeNow(PET, TODAY, 14)).isEmpty();
    }

    @Test
    void anAllAtOnceSwitchIsEnded() {
        FoodTransition replacement = FoodTransition.replacement(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                TODAY.minusDays(3), Instant.now(), OffsetDateTime.now());
        givenTransitions(replacement);

        assertThat(this.api.changeNow(PET, TODAY, 14)).contains(new FoodChangeNow(State.ENDED, TODAY.minusDays(3)));
    }

    @Test
    void noChangesGivesNothing() {
        givenTransitions();

        assertThat(this.api.changeNow(PET, TODAY, 14)).isEmpty();
    }

    @Test
    void theNewestStartedChangeWins() {
        FoodTransition tomorrow = planned(TODAY.plusDays(1));
        FoodTransition older = planned(TODAY.minusDays(2));
        givenTransitions(tomorrow, older);

        Optional<FoodChangeNow> change = this.api.changeNow(PET, TODAY, 14);

        assertThat(change).contains(new FoodChangeNow(State.RUNNING, TODAY.minusDays(2)));
    }

    private void givenTransitions(FoodTransition... transitions) {
        when(this.transitionRepository.findAllByPetIdOrderByStartOnDesc(PET)).thenReturn(List.of(transitions));
    }

    private static FoodTransition planned(LocalDate startOn) {
        return FoodTransition.planned(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TransitionPace.STANDARD, startOn,
                Instant.now());
    }
}
