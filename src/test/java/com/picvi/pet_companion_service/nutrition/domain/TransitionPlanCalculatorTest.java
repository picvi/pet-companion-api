package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.DayMix;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PaceStep;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PauseInterval;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.Plan;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanInput;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPlanCalculator.PlanStep;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TransitionPlanCalculatorTest {
    private static final LocalDate MON = LocalDate.of(2026, 10, 19);
    private static final List<PaceStep> STANDARD = List.of(new PaceStep(1, 2, 25), new PaceStep(2, 2, 50), new PaceStep(3, 2, 75), new PaceStep(4, 1, 100));
    private static final List<PaceStep> SLOW = List.of(new PaceStep(1, 3, 25), new PaceStep(2, 3, 50), new PaceStep(3, 3, 75), new PaceStep(4, 1, 100));

    private final TransitionPlanCalculator calculator = new TransitionPlanCalculator();

    private Plan plan(List<PaceStep> pace, Map<Integer, Integer> extensions, List<PauseInterval> pauses, Integer perMeal, LocalDate asOf) {
        return this.calculator.plan(new PlanInput(pace, MON, pauses, extensions, perMeal), asOf);
    }

    private static int[] grams(DayMix mix) {
        return new int[]{mix.step().oldGrams(), mix.step().newGrams()};
    }

    @Test
    void standardPlanFromMondayWithThirtyGramsAMeal() {
        Plan plan = this.plan(STANDARD, Map.of(), List.of(), 30, MON);

        assertThat(grams(plan.mixOn(MON).orElseThrow())).containsExactly(22, 8);
        assertThat(grams(plan.mixOn(MON.plusDays(1)).orElseThrow())).containsExactly(22, 8);
        assertThat(grams(plan.mixOn(MON.plusDays(2)).orElseThrow())).containsExactly(15, 15);
        assertThat(grams(plan.mixOn(MON.plusDays(3)).orElseThrow())).containsExactly(15, 15);
        assertThat(grams(plan.mixOn(MON.plusDays(4)).orElseThrow())).containsExactly(8, 22);
        assertThat(grams(plan.mixOn(MON.plusDays(5)).orElseThrow())).containsExactly(8, 22);
        assertThat(grams(plan.mixOn(MON.plusDays(6)).orElseThrow())).containsExactly(0, 30);
    }

    @Test
    void standardPlanLastsSevenDaysAndEndsOnSunday() {
        Plan plan = this.plan(STANDARD, Map.of(), List.of(), 30, MON);

        assertThat(plan.totalDays()).isEqualTo(7);
        assertThat(plan.lastDay()).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(plan.steps()).extracting(PlanStep::from).containsExactly(MON, MON.plusDays(2), MON.plusDays(4), MON.plusDays(6));
        assertThat(plan.steps()).extracting(PlanStep::to).containsExactly(MON.plusDays(1), MON.plusDays(3), MON.plusDays(5), MON.plusDays(6));
    }

    @Test
    void slowerPlanTakesTenDaysInThreeDaySteps() {
        Plan plan = this.plan(SLOW, Map.of(), List.of(), 30, MON);

        assertThat(plan.totalDays()).isEqualTo(10);
        assertThat(plan.lastDay()).isEqualTo(MON.plusDays(9));
        assertThat(plan.mixOn(MON.plusDays(2)).orElseThrow().step().order()).isEqualTo(1);
        assertThat(plan.mixOn(MON.plusDays(3)).orElseThrow().step().order()).isEqualTo(2);
    }

    @Test
    void dayOfPlanAndTotalDaysAreOneBased() {
        DayMix mix = this.plan(STANDARD, Map.of(), List.of(), 30, MON).mixOn(MON.plusDays(2)).orElseThrow();

        assertThat(mix.dayOfPlan()).isEqualTo(3);
        assertThat(mix.totalDays()).isEqualTo(7);
    }

    @Test
    void aLooseStoolOnDayThreeKeepsFiftyFiftyUntilDaySixAndMovesTheEndByTwoDays() {
        Plan plan = this.plan(STANDARD, Map.of(2, 2), List.of(), 30, MON.plusDays(2));

        for (int day = 2; day <= 5; day++) {
            assertThat(plan.mixOn(MON.plusDays(day)).orElseThrow().step().newPercent()).isEqualTo(50);
        }
        assertThat(plan.mixOn(MON.plusDays(6)).orElseThrow().step().newPercent()).isEqualTo(75);
        assertThat(plan.totalDays()).isEqualTo(9);
        assertThat(plan.lastDay()).isEqualTo(MON.plusDays(8));
        assertThat(plan.steps().get(1).extendedDays()).isEqualTo(2);
    }

    @Test
    void withoutAnAmountThereAreOnlyPercentages() {
        PlanStep first = this.plan(STANDARD, Map.of(), List.of(), null, MON).steps().get(0);

        assertThat(first.newPercent()).isEqualTo(25);
        assertThat(first.oldGrams()).isNull();
        assertThat(first.newGrams()).isNull();
    }

    @Test
    void theSmallerShareIsRoundedUp() {
        assertThat(this.calculator.split(30, 25)).containsExactly(22, 8);
        assertThat(this.calculator.split(30, 75)).containsExactly(8, 22);
        assertThat(this.calculator.split(30, 50)).containsExactly(15, 15);
        assertThat(this.calculator.split(25, 25)).containsExactly(19, 6);
        assertThat(this.calculator.split(30, 100)).containsExactly(0, 30);
    }

    @Test
    void nothingIsMixedBeforeTheStartOrAfterTheLastDay() {
        Plan plan = this.plan(STANDARD, Map.of(), List.of(), 30, MON);

        assertThat(plan.mixOn(MON.minusDays(1))).isEmpty();
        assertThat(plan.mixOn(MON.plusDays(7))).isEmpty();
        assertThat(plan.isOver(MON.plusDays(6))).isFalse();
        assertThat(plan.isOver(MON.plusDays(7))).isTrue();
        assertThat(plan.isOver(MON.minusDays(1))).isFalse();
    }

    @Test
    void aPauseHoldsTheStepItBeganOnAndPushesTheEnd() {
        List<PauseInterval> pauses = List.of(new PauseInterval(MON.plusDays(2), MON.plusDays(5)));
        Plan plan = this.plan(STANDARD, Map.of(), pauses, 30, MON.plusDays(6));

        assertThat(plan.mixOn(MON.plusDays(2)).orElseThrow().dayOfPlan()).isEqualTo(3);
        assertThat(plan.mixOn(MON.plusDays(3)).orElseThrow().dayOfPlan()).isEqualTo(3);
        assertThat(plan.mixOn(MON.plusDays(4)).orElseThrow().dayOfPlan()).isEqualTo(3);
        assertThat(plan.mixOn(MON.plusDays(5)).orElseThrow().dayOfPlan()).isEqualTo(3);
        assertThat(plan.mixOn(MON.plusDays(6)).orElseThrow().dayOfPlan()).isEqualTo(4);
        assertThat(plan.lastDay()).isEqualTo(MON.plusDays(9));
    }

    @Test
    void whileStillPausedTheDayStaysFrozen() {
        List<PauseInterval> pauses = List.of(new PauseInterval(MON.plusDays(2), null));
        Plan plan = this.plan(STANDARD, Map.of(), pauses, 30, MON.plusDays(4));

        assertThat(plan.mixOn(MON.plusDays(4)).orElseThrow().dayOfPlan()).isEqualTo(3);
        assertThat(plan.isOver(MON.plusDays(30))).isFalse();
    }

    @Test
    void aPlanViewedBeforeItStartsHasFutureDates() {
        Plan plan = this.plan(STANDARD, Map.of(), List.of(), 30, MON.minusDays(3));

        assertThat(plan.steps().get(0).from()).isEqualTo(MON);
        assertThat(plan.lastDay()).isEqualTo(MON.plusDays(6));
    }
}
