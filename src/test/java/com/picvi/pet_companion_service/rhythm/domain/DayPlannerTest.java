package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.Test;

import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import java.util.List;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS;
import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS_LARGE;
import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS_SMALL;
import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.TWENTY_WEEKS;
import static org.assertj.core.api.Assertions.assertThat;

class DayPlannerTest {
    private static final int WAKE = 7 * 60;
    private static final int BED = 22 * 60 + 30;
    private static final List<Integer> FOUR_MEALS = List.of(7 * 60 + 15, 12 * 60, 15 * 60 + 30, 18 * 60 + 30);

    private final DayPlanner planner = new DayPlanner();

    @Test
    void theWholeDayIsCoveredWithoutGapsOrOverlaps() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));

        assertThat(plan.blocks().getFirst().startMinute()).isZero();
        assertThat(plan.blocks().getLast().endMinute()).isEqualTo(1440);
        for (int index = 1; index < plan.blocks().size(); index++) {
            assertThat(plan.blocks().get(index).startMinute()).isEqualTo(plan.blocks().get(index - 1).endMinute());
        }
    }

    @Test
    void nightSurroundsTheDayAndTheEveningStartsThirtyMinutesBeforeBed() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));

        assertThat(plan.blocks().getFirst()).isEqualTo(new DayBlock(BlockKind.NIGHT, 0, WAKE));
        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.EVENING_WIND_DOWN, BED - 30, BED));
        assertThat(plan.blocks().getLast()).isEqualTo(new DayBlock(BlockKind.NIGHT, BED, 1440));
    }

    @Test
    void afterWakingPlayComesBeforeAWindDownAndANap() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null));

        assertThat(plan.blocks().subList(1, 4)).containsExactly(
                new DayBlock(BlockKind.PLAY, 420, 480),
                new DayBlock(BlockKind.WIND_DOWN, 480, 495),
                new DayBlock(BlockKind.NAP, 495, 585));
    }

    @Test
    void aMealDuringTheAwakeWindowStaysAtItsTime() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(7 * 60 + 15), null));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.MEAL, 435, 450));
        assertThat(plan.flags()).doesNotContain(PlanFlag.MEAL_MOVED_FOR_NAP);
    }

    @Test
    void aMealDueWhileSheSleepsWaitsUntilSheWakes() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));

        DayBlock nap = plan.blocks().stream()
                .filter(block -> block.kind() == BlockKind.NAP && block.startMinute() < 12 * 60 && block.endMinute() > 12 * 60)
                .findFirst().orElseThrow();
        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.MEAL, nap.endMinute(), nap.endMinute() + 15));
        assertThat(plan.flags()).contains(PlanFlag.MEAL_MOVED_FOR_NAP);
    }

    @Test
    void aMealDueExactlyWhenTheAwakeWindowEndsIsServedBeforeTheNap() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(495), null));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.MEAL, 495, 510), new DayBlock(BlockKind.NAP, 510, 600));
        assertThat(plan.flags()).doesNotContain(PlanFlag.MEAL_MOVED_FOR_NAP);
    }

    @Test
    void everyMealIsPlannedExactlyOnce() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));

        assertThat(plan.blocks().stream().filter(block -> block.kind() == BlockKind.MEAL)).hasSize(4);
    }

    @Test
    void aCustomAwakeWindowChangesHowLongPlayLasts() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), 45));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.NAP, 465, 555));
    }

    @Test
    void thereIsAToiletTripAfterWakingAfterEveryNapAndBeforeBed() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null));

        List<Integer> napEnds = plan.blocks().stream().filter(block -> block.kind() == BlockKind.NAP)
                .map(DayBlock::endMinute).filter(end -> end < BED - 30).toList();
        assertThat(plan.toiletTrips()).contains(WAKE).containsAll(napEnds).contains(BED - 10);
    }

    @Test
    void toiletTripsAreNeverMoreThanTheIntervalApartWhileAwake() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), 90));

        for (int index = 1; index < plan.toiletTrips().size(); index++) {
            int from = plan.toiletTrips().get(index - 1);
            int to = plan.toiletTrips().get(index);
            boolean sleepsBetween = plan.blocks().stream().anyMatch(block -> block.kind() == BlockKind.NAP
                    && block.startMinute() >= from && block.endMinute() <= to);
            assertThat(sleepsBetween || to - from <= ELEVEN_WEEKS.toiletIntervalMinutes()).isTrue();
        }
    }

    @Test
    void nightTripsAreSpreadAcrossTheNight() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));

        assertThat(plan.nightTrips()).containsExactly(2 * 60 + 45);
    }

    @Test
    void olderPuppiesHaveNoNightTrips() {
        DayPlan plan = this.planner.plan(input(TWENTY_WEEKS, List.of(), null));

        assertThat(plan.nightTrips()).isEmpty();
    }

    @Test
    void theRuleDecidesWhichNotesApply() {
        assertThat(this.planner.plan(input(ELEVEN_WEEKS_SMALL, FOUR_MEALS, null)).flags()).contains(PlanFlag.MEAL_SAFETY_NOTE);
        assertThat(this.planner.plan(input(ELEVEN_WEEKS_LARGE, FOUR_MEALS, null)).flags()).contains(PlanFlag.SOFT_GROUND_PLAY);
        assertThat(this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null)).flags())
                .doesNotContain(PlanFlag.MEAL_SAFETY_NOTE, PlanFlag.SOFT_GROUND_PLAY);
    }

    @Test
    void wakingEarlierThanPlannedStartsTheDayFromThatMoment() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null, List.of(new LoggedEvent(LogType.WAKE, 400))));

        assertThat(plan.blocks().getFirst()).isEqualTo(new DayBlock(BlockKind.NIGHT, 0, 400));
        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.NAP, 475, 565));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void fallingAsleepStartsANapAndTheRestOfTheDayFollowsIt() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null, List.of(new LoggedEvent(LogType.SLEEP, 600))));

        assertThat(plan.blocks()).contains(
                new DayBlock(BlockKind.PLAY, 585, 600),
                new DayBlock(BlockKind.NAP, 600, 690),
                new DayBlock(BlockKind.MEAL, 720, 735));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void fallingAsleepLaterThanPlannedMeansSheWasAwakeUntilThen() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null, List.of(new LoggedEvent(LogType.SLEEP, 540))));

        assertThat(plan.blocks()).contains(
                new DayBlock(BlockKind.WIND_DOWN, 480, 495),
                new DayBlock(BlockKind.PLAY, 495, 540),
                new DayBlock(BlockKind.NAP, 540, 630));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void theLatestSleepOrWakeLogDecidesWhereTheDayRestarts() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null,
                List.of(new LoggedEvent(LogType.SLEEP, 600), new LoggedEvent(LogType.WAKE, 630))));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.PLAY, 585, 690), new DayBlock(BlockKind.NAP, 705, 795));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void aMealEatenAheadOfTimeIsNotPlannedAgain() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null,
                List.of(new LoggedEvent(LogType.MEAL, 700), new LoggedEvent(LogType.WAKE, 705))));

        assertThat(plan.blocks().stream().filter(block -> block.kind() == BlockKind.MEAL)
                .map(DayBlock::startMinute)).hasSize(3).doesNotContain(720, 750);
    }

    @Test
    void aMealLogFarFromEveryPlannedMealDoesNotRemoveOne() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null,
                List.of(new LoggedEvent(LogType.MEAL, 540), new LoggedEvent(LogType.WAKE, 610))));

        assertThat(plan.blocks().stream().filter(block -> block.kind() == BlockKind.MEAL)).hasSize(4);
    }

    @Test
    void mealAndToiletLogsAloneDoNotChangeThePlan() {
        DayPlan scheduled = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));
        DayPlan logged = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null,
                List.of(new LoggedEvent(LogType.MEAL, 440), new LoggedEvent(LogType.TOILET, 460))));

        assertThat(logged).isEqualTo(scheduled);
    }

    @Test
    void aLogInTheEveningWindDownDoesNotChangeThePlan() {
        DayPlan scheduled = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null));
        DayPlan logged = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null, List.of(new LoggedEvent(LogType.WAKE, 1330))));

        assertThat(logged).isEqualTo(scheduled);
    }

    @Test
    void anEventIsPlannedAroundWithACalmWindDownBeforeAndANapAfter() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null, List.of(), List.of(new FixedEvent(600, 660))));

        assertThat(plan.blocks()).contains(
                new DayBlock(BlockKind.NAP, 495, 585),
                new DayBlock(BlockKind.WIND_DOWN, 585, 600),
                new DayBlock(BlockKind.EVENT, 600, 660),
                new DayBlock(BlockKind.NAP, 660, 750));
        assertThat(plan.toiletTrips()).contains(660);
        assertCoversTheWholeDay(plan);
    }

    @Test
    void aNapIsCutShortSoSheIsAwakeForTheEvent() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null, List.of(), List.of(new FixedEvent(540, 600))));

        assertThat(plan.blocks()).contains(
                new DayBlock(BlockKind.NAP, 495, 525),
                new DayBlock(BlockKind.WIND_DOWN, 525, 540),
                new DayBlock(BlockKind.EVENT, 540, 600));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void aMealDueDuringAnEventIsServedRightAfterIt() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(630), null, List.of(), List.of(new FixedEvent(600, 660))));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.MEAL, 660, 675), new DayBlock(BlockKind.NAP, 675, 765));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void anEventAfterALoggedWakeUpIsStillPlanned() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null,
                List.of(new LoggedEvent(LogType.WAKE, 400)), List.of(new FixedEvent(600, 660))));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.EVENT, 600, 660));
        assertCoversTheWholeDay(plan);
    }

    @Test
    void severalEventsAreEachPlannedOnce() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, FOUR_MEALS, null, List.of(),
                List.of(new FixedEvent(600, 660), new FixedEvent(900, 960))));

        assertThat(plan.blocks().stream().filter(block -> block.kind() == BlockKind.EVENT)).hasSize(2);
        assertCoversTheWholeDay(plan);
    }

    @Test
    void anEventEndingAfterTheEveningStartsIsCutBackToIt() {
        DayPlan plan = this.planner.plan(input(ELEVEN_WEEKS, List.of(), null, List.of(), List.of(new FixedEvent(1200, 1340))));

        assertThat(plan.blocks()).contains(new DayBlock(BlockKind.EVENT, 1200, 1320));
        assertCoversTheWholeDay(plan);
    }

    private static void assertCoversTheWholeDay(DayPlan plan) {
        assertThat(plan.blocks().getFirst().startMinute()).isZero();
        assertThat(plan.blocks().getLast().endMinute()).isEqualTo(1440);
        for (int index = 1; index < plan.blocks().size(); index++) {
            assertThat(plan.blocks().get(index).startMinute()).isEqualTo(plan.blocks().get(index - 1).endMinute());
        }
    }

    private static DayPlanInput input(RhythmRule rule, List<Integer> meals, Integer customAwake) {
        return input(rule, meals, customAwake, List.of());
    }

    private static DayPlanInput input(RhythmRule rule, List<Integer> meals, Integer customAwake, List<LoggedEvent> logs) {
        return input(rule, meals, customAwake, logs, List.of());
    }

    private static DayPlanInput input(RhythmRule rule, List<Integer> meals, Integer customAwake, List<LoggedEvent> logs,
                                      List<FixedEvent> events) {
        return new DayPlanInput(rule, new DaySettings(WAKE, BED, meals, customAwake), logs, events);
    }
}
