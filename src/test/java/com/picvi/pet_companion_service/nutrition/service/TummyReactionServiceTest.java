package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.ChangeView;
import com.picvi.pet_companion_service.nutrition.domain.ReactionKind;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PaceStep;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanInput;
import com.picvi.pet_companion_service.nutrition.domain.TummyReaction;
import com.picvi.pet_companion_service.nutrition.domain.TummyReactionPolicy;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransition;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionExtension;
import com.picvi.pet_companion_service.nutrition.entity.FoodTransitionPause;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionExtensionRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionPauseRepository;
import com.picvi.pet_companion_service.nutrition.repository.FoodTransitionRepository;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import com.picvi.pet_companion_service.rhythm.api.RhythmApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TummyReactionServiceTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate MON = LocalDate.of(2026, 10, 19);
    private static final LocalDate WED = MON.plusDays(2);

    @Mock
    private PetLogApi petLogApi;
    @Mock
    private FoodChangeReader changeReader;
    @Mock
    private FoodChangeActions actions;
    @Mock
    private FoodTransitionRepository transitionRepository;
    @Mock
    private FoodTransitionExtensionRepository extensionRepository;
    @Mock
    private FoodTransitionPauseRepository pauseRepository;
    @Mock
    private RhythmApi rhythmApi;

    private TummyReactionService service;
    private FoodTransition transition;

    @BeforeEach
    void setUp() {
        this.service = new TummyReactionService(this.petLogApi, new TummyReactionPolicy(), this.changeReader, this.actions,
                this.transitionRepository, this.extensionRepository, this.pauseRepository, this.rhythmApi);
        this.transition = FoodTransition.planned(PET, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), TransitionPace.STANDARD, MON, Instant.now());
        ReflectionTestUtils.setField(this.transition, "id", UUID.randomUUID());
        lenient().when(this.transitionRepository.findOpen(eq(PET), any())).thenReturn(Optional.of(this.transition));
        lenient().when(this.rhythmApi.mealsPerDay(PET, WED)).thenReturn(4);
        TransitionPlanCalculator calculator = new TransitionPlanCalculator();
        lenient().when(this.changeReader.view(this.transition, WED, 4)).thenReturn(new ChangeView(this.transition, calculator.plan(
                new PlanInput(List.of(new PaceStep(1, 2, 25), new PaceStep(2, 2, 50), new PaceStep(3, 2, 75), new PaceStep(4, 1, 100)),
                        MON, List.of(), Map.of(), 30), WED), null, null));
        lenient().when(this.petLogApi.lastMeals(PET, 2)).thenReturn(List.of());
        lenient().when(this.pauseRepository.findAllByTransitionIdOrderByPausedOnAsc(this.transition.getId())).thenReturn(List.of());
    }

    private static LogSnapshot looseStool(UUID id) {
        return new LogSnapshot(id, LogType.TOILET, LogDetail.POO, 6, null, Set.of(), WED, 600, null, null, null, null);
    }

    private static LogSnapshot vomiting(UUID id) {
        return new LogSnapshot(id, LogType.SYMPTOM, LogDetail.VOMITING, null, Wellbeing.USUAL, Set.of(), WED, 700, null, null, null, null);
    }

    @Test
    void aLooseStoolSlowsTheStepDownAndRemembersWhichLog() {
        UUID log = UUID.randomUUID();
        when(this.petLogApi.logsSince(PET, WED)).thenReturn(List.of(looseStool(log)));

        this.service.apply(PET, WED);

        verify(this.actions).slowDown(this.transition, WED, log);
    }

    @Test
    void stoolBeforeTheCurrentStepDoesNotCount() {
        when(this.petLogApi.logsSince(PET, WED)).thenReturn(List.of());

        this.service.apply(PET, WED);

        verify(this.actions, never()).slowDown(any(), any(), any());
    }

    @Test
    void aSlowDownTheOwnerUndidIsNotAppliedAgainOnRead() {
        UUID log = UUID.randomUUID();
        when(this.petLogApi.logsSince(PET, WED)).thenReturn(List.of(looseStool(log)));
        FoodTransitionExtension undone = new FoodTransitionExtension(this.transition.getId(), 2, 2, log, OffsetDateTime.now());
        undone.tombstone();
        when(this.extensionRepository.findByTransitionIdAndStepOrder(this.transition.getId(), 2)).thenReturn(Optional.of(undone));

        this.service.apply(PET, WED);

        verify(this.actions, never()).slowDown(any(), any(), any());
        assertThat(this.service.reactionOn(this.transition, WED, 4)).isEmpty();
    }

    @Test
    void vomitingPausesThePlanOnce() {
        when(this.petLogApi.logsSince(PET, WED)).thenReturn(List.of(vomiting(UUID.randomUUID())));

        this.service.apply(PET, WED);
        verify(this.actions).pause(this.transition, WED);

        when(this.pauseRepository.findAllByTransitionIdOrderByPausedOnAsc(this.transition.getId()))
                .thenReturn(List.of(new FoodTransitionPause(this.transition.getId(), WED)));
        this.service.apply(PET, WED);
        verify(this.actions).pause(this.transition, WED);
    }

    @Test
    void aBloodyStoolIsAlsoARedFlag() {
        LogSnapshot blood = new LogSnapshot(UUID.randomUUID(), LogType.TOILET, LogDetail.POO, 4, null, Set.of(LogFlag.BLOOD), WED, 600, null, null, null, null);
        when(this.petLogApi.logsSince(PET, WED)).thenReturn(List.of(blood));

        assertThat(this.service.reactionOn(this.transition, WED, 4)).map(TummyReaction::kind).contains(ReactionKind.PAUSE_AND_VET);
    }

    @Test
    void aPausedChangeIsNotPausedAgainAndAPauseIsNotAppliedToAnAlreadyPausedOne() {
        this.transition.pause();
        when(this.transitionRepository.findOpen(eq(PET), any())).thenReturn(Optional.of(this.transition));

        this.service.apply(PET, WED);

        verify(this.actions, never()).pause(any(), any());
    }

    @Test
    void nothingRunningMeansNothingToDo() {
        when(this.transitionRepository.findOpen(eq(PET), any())).thenReturn(Optional.empty());

        this.service.apply(PET, WED);

        verify(this.petLogApi, never()).logsSince(any(), any());
    }
}
