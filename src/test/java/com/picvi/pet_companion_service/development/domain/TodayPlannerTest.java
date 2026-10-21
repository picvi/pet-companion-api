package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.development.ActivityCatalogueFixture;
import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TodayPlannerTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(2026, 1, 1);
    private static final int EIGHT_WEEKS_IN_DAYS = 8 * 7;
    private static final int TWELVE_WEEKS_IN_DAYS = 12 * 7;
    private static final List<ActivityDefinition> CATALOGUE = ActivityCatalogueFixture.load();
    private static final Map<String, ActivityDefinition> CATALOGUE_BY_CODE = CATALOGUE.stream()
            .collect(Collectors.toMap(ActivityDefinition::code, Function.identity()));

    private final TodayPlanner planner = new TodayPlanner(
            new LifeStageCalculator(), new StageProgressCalculator(), new VaccinationPrompt());
    private final ProgressRules rules = new ProgressRules();

    @Test
    void welcomeOnFirstTwoDaysAfterPetCreated() {
        for (int daysSinceCreated = 0; daysSinceCreated < 2; daysSinceCreated++) {
            TodayPlan plan = planner.plan(puppy().atAgeDays(EIGHT_WEEKS_IN_DAYS + daysSinceCreated).build());

            assertThat(plan.focus().type()).isEqualTo(FocusType.WELCOME);
            assertThat(plan.focus().activity()).isNull();
            assertThat(plan.focus().doneToday()).isFalse();
            assertThat(plan.focus().reason().code()).isEqualTo(FocusReasonCode.WELCOME);
            assertThat(plan.focus().reason().ageWeeks()).isEqualTo(8);
            assertThat(plan.focus().reason().weeksLeftInWindow()).isEqualTo(8);
        }

        TodayPlan thirdDay = planner.plan(puppy().atAgeDays(EIGHT_WEEKS_IN_DAYS + 2).build());

        assertThat(thirdDay.focus().type()).isNotEqualTo(FocusType.WELCOME);
    }

    @Test
    void welcomePlanContainsHomeBasics() {
        TodayPlan plan = planner.plan(puppy().atAgeDays(EIGHT_WEEKS_IN_DAYS).build());

        assertThat(plan.plan()).extracting(item -> item.activity().code())
                .containsExactly("TOILET_ROUTINE", "SAFE_SPACE", "NAP_ROUTINE");
        assertThat(plan.plan()).allSatisfy(item -> {
            assertThat(item.reason()).isEqualTo(PlanReason.HOME_BASICS);
            assertThat(item.activity().category()).isEqualTo(ActivityCategory.HOME_LIFE);
        });
    }

    @Test
    void stageIntroOnFirstTwoDaysOfFindingConfidence() {
        int firstDayOfStage = 10 * 7;

        assertThat(planner.plan(puppy().atAgeDays(firstDayOfStage).build()).focus().type())
                .isEqualTo(FocusType.STAGE_INTRO);
        assertThat(planner.plan(puppy().atAgeDays(firstDayOfStage + 1).build()).focus().type())
                .isEqualTo(FocusType.STAGE_INTRO);
        assertThat(planner.plan(puppy().atAgeDays(firstDayOfStage + 2).build()).focus().type())
                .isEqualTo(FocusType.ACTIVITY);
    }

    @Test
    void noStageIntroForHomecoming() {
        TodayPlan plan = planner.plan(puppy().createdDaysBeforeBirth(30).atAgeDays(1).build());

        assertThat(plan.stage().stage()).isEqualTo(DevelopmentStage.HOMECOMING);
        assertThat(plan.focus().type()).isNotEqualTo(FocusType.STAGE_INTRO);
    }

    @Test
    void newlyUnlockedDayAfterSeriesComplete() {
        TodayPlan plan = planner.plan(puppy().atAgeWeeks(17)
                .vaccination(VaccinationStatus.SERIES_COMPLETE, 1).build());

        assertThat(plan.focus().type()).isEqualTo(FocusType.NEWLY_UNLOCKED);
        assertThat(plan.focus().activity().code()).isEqualTo("PLACE_FIRST_PARK_WALK");
        assertThat(plan.focus().activity().vaccinationRequirement()).isEqualTo(VaccinationRequirement.SERIES_COMPLETE);
    }

    @Test
    void noNewlyUnlockedTwoDaysAfterSeriesComplete() {
        TodayPlan plan = planner.plan(puppy().atAgeWeeks(17)
                .vaccination(VaccinationStatus.SERIES_COMPLETE, 2).build());

        assertThat(plan.focus().type()).isNotEqualTo(FocusType.NEWLY_UNLOCKED);
    }

    @Test
    void socialisationFocusDuringWindow() {
        TodayPlan plan = planner.plan(puppy().atAgeWeeks(12).build());

        assertThat(plan.focus().type()).isEqualTo(FocusType.ACTIVITY);
        assertThat(plan.focus().activity().kind()).isEqualTo(ActivityKind.SOCIALIZATION);
        assertThat(plan.focus().reason().code()).isEqualTo(FocusReasonCode.SOCIALISATION_WINDOW);
        assertThat(plan.focus().reason().ageWeeks()).isEqualTo(12);
        assertThat(plan.focus().reason().weeksLeftInWindow()).isEqualTo(4);
        assertThat(plan.focus().reason().categoryCompleted()).isZero();
        assertThat(plan.focus().reason().categoryTotal()).isPositive();
    }

    @Test
    void categoryCountersUseRealCompletions() {
        PlannerInput withoutProgress = puppy().atAgeWeeks(12).build();
        ActivityDefinition focus = planner.plan(withoutProgress).focus().activity();
        ActivityDefinition completedSibling = CATALOGUE.stream()
                .filter(activity -> activity.category() == focus.category())
                .filter(activity -> !activity.code().equals(focus.code()))
                .filter(activity -> activity.coversAgeWeeks(12))
                .filter(activity -> activity.vaccinationRequirement() == VaccinationRequirement.NONE)
                .findFirst().orElseThrow();

        TodayPlan plan = planner.plan(puppy().atAgeWeeks(12)
                .progress(completedSibling.code(), outcome(completedSibling, Outcome.CALM, TWELVE_WEEKS_IN_DAYS - 3)).build());

        assertThat(plan.focus().reason().categoryCompleted()).isEqualTo(1);
    }

    @Test
    void focusRespectsVaccinationRequirement() {
        for (VaccinationStatus status : List.of(VaccinationStatus.NONE, VaccinationStatus.UNKNOWN)) {
            for (int ageDays = EIGHT_WEEKS_IN_DAYS; ageDays < 26 * 7; ageDays++) {
                TodayPlan plan = planner.plan(puppy().atAgeDays(ageDays).vaccination(status, null).build());

                assertThat(activitiesOf(plan))
                        .as("age %d days, status %s", ageDays, status)
                        .allSatisfy(activity -> assertThat(activity.vaccinationRequirement())
                                .isEqualTo(VaccinationRequirement.NONE));
            }
        }
        for (int ageDays = EIGHT_WEEKS_IN_DAYS; ageDays < 26 * 7; ageDays++) {
            TodayPlan plan = planner.plan(puppy().atAgeDays(ageDays)
                    .vaccination(VaccinationStatus.FIRST_DOSE, null).build());

            assertThat(activitiesOf(plan))
                    .as("age %d days, first dose", ageDays)
                    .allSatisfy(activity -> assertThat(activity.vaccinationRequirement())
                            .isNotEqualTo(VaccinationRequirement.SERIES_COMPLETE));
        }
    }

    @Test
    void skillFocusAfterWindow() {
        TodayPlan plan = planner.plan(puppy().atAgeWeeks(20).build());

        assertThat(plan.focus().type()).isEqualTo(FocusType.ACTIVITY);
        assertThat(plan.focus().activity().kind()).isEqualTo(ActivityKind.SKILL);
        assertThat(plan.focus().reason().code()).isEqualTo(FocusReasonCode.SKILL_PRACTICE);
    }

    @Test
    void stageTipWhenNothingIsSuggestable() {
        PlannerInput input = puppy().size(SizeCategory.SMALL).atAgeWeeks(100).build();

        TodayPlan plan = planner.plan(input);

        assertThat(plan.stage().stage()).isEqualTo(DevelopmentStage.YOUNG_ADULT);
        assertThat(plan.focus().type()).isEqualTo(FocusType.STAGE_TIP);
        assertThat(plan.focus().activity()).isNull();
        assertThat(plan.focus().doneToday()).isFalse();
        assertThat(plan.focus().reason().code()).isEqualTo(FocusReasonCode.STAGE_TIP);
        assertThat(plan.focus().reason().tipIndex()).isEqualTo(plan.stage().ageDays());
        assertThat(plan.plan()).isEmpty();
    }

    @Test
    void focusUnchangedAfterCompletingItToday() {
        for (int ageWeeks : List.of(12, 20)) {
            PlannerInput before = puppy().atAgeWeeks(ageWeeks).build();
            Focus focusBefore = planner.plan(before).focus();
            Outcome completing = focusBefore.activity().kind() == ActivityKind.SOCIALIZATION
                    ? Outcome.CALM : Outcome.LEARNED;

            TodayPlan after = planner.plan(puppy().atAgeWeeks(ageWeeks)
                    .progress(focusBefore.activity().code(), outcome(focusBefore.activity(), completing, ageWeeks * 7)).build());

            assertThat(focusBefore.doneToday()).isFalse();
            assertThat(after.focus().activity().code()).isEqualTo(focusBefore.activity().code());
            assertThat(after.focus().doneToday()).isTrue();
        }
    }

    @Test
    void swapTakesEffectImmediately() {
        PlannerInput before = puppy().atAgeWeeks(12).build();
        ActivityDefinition focusBefore = planner.plan(before).focus().activity();
        ProgressState snoozed = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_TODAY, BIRTH_DATE.plusDays(TWELVE_WEEKS_IN_DAYS));

        TodayPlan after = planner.plan(puppy().atAgeWeeks(12).progress(focusBefore.code(), snoozed).build());

        assertThat(after.focus().activity().code()).isNotEqualTo(focusBefore.code());
        assertThat(after.focus().doneToday()).isFalse();
    }

    @Test
    void notPossibleActivityIsNeverSuggested() {
        PlannerInput before = puppy().atAgeWeeks(12).build();
        ActivityDefinition hidden = planner.plan(before).focus().activity();
        ProgressState notPossible = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_POSSIBLE, BIRTH_DATE.plusDays(TWELVE_WEEKS_IN_DAYS));

        for (int ageDays = EIGHT_WEEKS_IN_DAYS + 2; ageDays < 26 * 7; ageDays++) {
            TodayPlan plan = planner.plan(puppy().atAgeDays(ageDays).progress(hidden.code(), notPossible).build());

            assertThat(activitiesOf(plan)).as("age %d days", ageDays)
                    .extracting(ActivityDefinition::code).doesNotContain(hidden.code());
        }
    }

    @Test
    void scaredActivityRestsSevenDays() {
        int scaredAtAgeDays = 12 * 7;
        ActivityDefinition scared = planner.plan(puppy().atAgeDays(scaredAtAgeDays).build()).focus().activity();
        ProgressState scaredState = outcome(scared, Outcome.SCARED, scaredAtAgeDays);

        for (int daysLater = 1; daysLater <= 6; daysLater++) {
            TodayPlan plan = planner.plan(puppy().atAgeDays(scaredAtAgeDays + daysLater)
                    .progress(scared.code(), scaredState).build());

            assertThat(activitiesOf(plan)).as("%d days after SCARED", daysLater)
                    .extracting(ActivityDefinition::code).doesNotContain(scared.code());
        }
    }

    @Test
    void easierActivityReplacesScaredOneInSameCategory() {
        int ageDays = 12 * 7;
        ActivityDefinition scared = planner.plan(puppy().atAgeDays(ageDays).build()).focus().activity();
        ProgressState scaredYesterday = outcome(scared, Outcome.SCARED, ageDays - 1);

        TodayPlan plan = planner.plan(puppy().atAgeDays(ageDays).progress(scared.code(), scaredYesterday).build());

        assertThat(plan.focus().activity().code()).isNotEqualTo(scared.code());
        assertThat(plan.focus().activity().category()).isEqualTo(scared.category());
    }

    @Test
    void planHasAtMostThreeItemsAndNeverTheFocus() {
        for (SizeCategory size : SizeCategory.values()) {
            for (int ageDays = EIGHT_WEEKS_IN_DAYS; ageDays < 110 * 7; ageDays++) {
                TodayPlan plan = planner.plan(puppy().size(size).atAgeDays(ageDays).build());

                List<String> planCodes = plan.plan().stream().map(item -> item.activity().code()).toList();
                assertThat(planCodes).as("%s age %d days", size, ageDays).hasSizeLessThanOrEqualTo(3);
                assertThat(new LinkedHashSet<>(planCodes)).hasSameSizeAs(planCodes);
                if (plan.focus().activity() != null) {
                    assertThat(planCodes).doesNotContain(plan.focus().activity().code());
                }
            }
        }
    }

    @Test
    void weeklyWeighInAppearsWhenDueAndStaysListedWhenDoneToday() {
        PlanItem due = findItem(planner.plan(puppy().atAgeWeeks(12).build()), "WEEKLY_WEIGH_IN");
        ActivityDefinition weighIn = CATALOGUE_BY_CODE.get("WEEKLY_WEIGH_IN");

        PlanItem doneToday = findItem(planner.plan(puppy().atAgeWeeks(12)
                .progress("WEEKLY_WEIGH_IN", outcome(weighIn, Outcome.PRACTISED, TWELVE_WEEKS_IN_DAYS)).build()), "WEEKLY_WEIGH_IN");

        assertThat(due.reason()).isEqualTo(PlanReason.RECURRING_DUE);
        assertThat(due.doneToday()).isFalse();
        assertThat(doneToday.reason()).isEqualTo(PlanReason.RECURRING_DUE);
        assertThat(doneToday.doneToday()).isTrue();
    }

    @Test
    void weeklyWeighInDisappearsUntilNextWeek() {
        ActivityDefinition weighIn = CATALOGUE_BY_CODE.get("WEEKLY_WEIGH_IN");

        for (int daysLater = 1; daysLater <= 6; daysLater++) {
            TodayPlan plan = planner.plan(puppy().atAgeDays(TWELVE_WEEKS_IN_DAYS + daysLater)
                    .progress("WEEKLY_WEIGH_IN", outcome(weighIn, Outcome.PRACTISED, TWELVE_WEEKS_IN_DAYS)).build());

            assertThat(plan.plan()).extracting(item -> item.activity().code()).doesNotContain("WEEKLY_WEIGH_IN");
        }
        TodayPlan dueAgain = planner.plan(puppy().atAgeDays(TWELVE_WEEKS_IN_DAYS + 7)
                .progress("WEEKLY_WEIGH_IN", outcome(weighIn, Outcome.PRACTISED, TWELVE_WEEKS_IN_DAYS)).build());

        assertThat(dueAgain.plan()).extracting(item -> item.activity().code()).contains("WEEKLY_WEIGH_IN");
    }

    @Test
    void snoozedWeeklyActivityIsRemovedFromPlanToday() {
        ProgressState snoozed = rules.applySkip(ProgressState.notStarted(), SkipReason.NOT_TODAY, BIRTH_DATE.plusDays(TWELVE_WEEKS_IN_DAYS));

        TodayPlan plan = planner.plan(puppy().atAgeWeeks(12).progress("WEEKLY_WEIGH_IN", snoozed).build());

        assertThat(plan.plan()).extracting(item -> item.activity().code()).doesNotContain("WEEKLY_WEIGH_IN");
    }

    @Test
    void worriedCardAfterTwoScaredWithinAWeek() {
        TodayPlan plan = planner.plan(puppy().atAgeWeeks(12)
                .progress("PEOPLE_CALM_VISITOR", scaredOn("PEOPLE_CALM_VISITOR", TWELVE_WEEKS_IN_DAYS - 4))
                .progress("SOUND_DOORBELL", scaredOn("SOUND_DOORBELL", TWELVE_WEEKS_IN_DAYS - 1)).build());

        assertThat(plan.showWorriedCard()).isTrue();
    }

    @Test
    void worriedCardNotShownThreeDaysAfterLastScared() {
        TodayPlan plan = planner.plan(puppy().atAgeWeeks(12)
                .progress("PEOPLE_CALM_VISITOR", scaredOn("PEOPLE_CALM_VISITOR", TWELVE_WEEKS_IN_DAYS - 5))
                .progress("SOUND_DOORBELL", scaredOn("SOUND_DOORBELL", TWELVE_WEEKS_IN_DAYS - 3)).build());

        assertThat(plan.showWorriedCard()).isFalse();
    }

    @Test
    void worriedCardNeedsTwoRecentScaredActivities() {
        TodayPlan oneOnly = planner.plan(puppy().atAgeWeeks(12)
                .progress("SOUND_DOORBELL", scaredOn("SOUND_DOORBELL", TWELVE_WEEKS_IN_DAYS)).build());
        TodayPlan oneTooOld = planner.plan(puppy().atAgeWeeks(12)
                .progress("PEOPLE_CALM_VISITOR", scaredOn("PEOPLE_CALM_VISITOR", TWELVE_WEEKS_IN_DAYS - 7))
                .progress("SOUND_DOORBELL", scaredOn("SOUND_DOORBELL", TWELVE_WEEKS_IN_DAYS)).build());

        assertThat(oneOnly.showWorriedCard()).isFalse();
        assertThat(oneTooOld.showWorriedCard()).isFalse();
    }

    @Test
    void vaccinationPromptDueFollowsThresholds() {
        assertThat(planner.plan(puppy().atAgeWeeks(15).build()).vaccinationPromptDue()).isFalse();
        assertThat(planner.plan(puppy().atAgeWeeks(16).build()).vaccinationPromptDue()).isTrue();
        assertThat(planner.plan(puppy().atAgeWeeks(16).promptCount(1).build()).vaccinationPromptDue()).isFalse();
        assertThat(planner.plan(puppy().atAgeWeeks(16).vaccination(VaccinationStatus.SERIES_COMPLETE, null).build())
                .vaccinationPromptDue()).isFalse();
    }

    @Test
    void stageProgressCountsRealCompletions() {
        ActivityDefinition completed = CATALOGUE_BY_CODE.get("PEOPLE_CALM_VISITOR");

        TodayPlan plan = planner.plan(puppy().atAgeWeeks(12)
                .progress(completed.code(), outcome(completed, Outcome.CALM, TWELVE_WEEKS_IN_DAYS - 2)).build());

        assertThat(plan.stageProgress().completed()).isEqualTo(1);
        assertThat(plan.stageProgress().total()).isGreaterThan(1);
    }

    @Test
    void ignoredOwnerNeverSeesSameActivityTwoDaysInARow() {
        for (SizeCategory size : List.of(SizeCategory.SMALL, SizeCategory.GIANT)) {
            String previousCode = null;
            for (int ageDays = EIGHT_WEEKS_IN_DAYS; ageDays <= 60 * 7; ageDays++) {
                ActivityDefinition focus = planner.plan(puppy().size(size).atAgeDays(ageDays).build()).focus().activity();
                String code = focus == null ? null : focus.code();

                if (code != null) {
                    assertThat(code).as("%s, age %d days", size, ageDays).isNotEqualTo(previousCode);
                }
                previousCode = code;
            }
        }
    }

    @Test
    void ignoredOwnerCoversEverySocialisationCategoryWithinTwoWeeks() {
        Set<ActivityCategory> covered = new LinkedHashSet<>();
        for (int ageDays = EIGHT_WEEKS_IN_DAYS; ageDays < EIGHT_WEEKS_IN_DAYS + 14; ageDays++) {
            ActivityDefinition focus = planner.plan(puppy().atAgeDays(ageDays)
                    .vaccination(VaccinationStatus.NONE, null).build()).focus().activity();
            if (focus != null && focus.kind() == ActivityKind.SOCIALIZATION) {
                covered.add(focus.category());
            }
        }

        Set<ActivityCategory> available = CATALOGUE.stream()
                .filter(activity -> activity.kind() == ActivityKind.SOCIALIZATION)
                .filter(activity -> activity.vaccinationRequirement() == VaccinationRequirement.NONE)
                .filter(activity -> activity.startWeek() < 10)
                .map(ActivityDefinition::category)
                .collect(Collectors.toSet());

        assertThat(covered).containsAll(available);
    }

    private List<ActivityDefinition> activitiesOf(TodayPlan plan) {
        List<ActivityDefinition> activities = new ArrayList<>();
        if (plan.focus().activity() != null) {
            activities.add(plan.focus().activity());
        }
        plan.plan().forEach(item -> activities.add(item.activity()));
        return activities;
    }

    private PlanItem findItem(TodayPlan plan, String code) {
        return plan.plan().stream().filter(item -> item.activity().code().equals(code)).findFirst().orElseThrow();
    }

    private ProgressState scaredOn(String code, int onAgeDays) {
        return outcome(CATALOGUE_BY_CODE.get(code), Outcome.SCARED, onAgeDays);
    }

    private ProgressState outcome(ActivityDefinition activity, Outcome outcome, int onAgeDays) {
        return rules.applyOutcome(ProgressState.notStarted(), activity, outcome, BIRTH_DATE.plusDays(onAgeDays));
    }

    private static Scenario puppy() {
        return new Scenario();
    }

    private static final class Scenario {
        private int ageDays = EIGHT_WEEKS_IN_DAYS;
        private LocalDate petCreatedOn = BIRTH_DATE.plusDays(EIGHT_WEEKS_IN_DAYS);
        private SizeCategory size = SizeCategory.MEDIUM;
        private VaccinationStatus vaccinationStatus = VaccinationStatus.NONE;
        private LocalDate vaccinationStatusChangedOn;
        private int promptCount;
        private final Map<String, ProgressState> progress = new HashMap<>();

        Scenario atAgeDays(int ageDays) {
            this.ageDays = ageDays;
            return this;
        }

        Scenario atAgeWeeks(int ageWeeks) {
            return atAgeDays(ageWeeks * 7);
        }

        Scenario createdDaysBeforeBirth(int days) {
            this.petCreatedOn = BIRTH_DATE.minusDays(days);
            return this;
        }

        Scenario size(SizeCategory size) {
            this.size = size;
            return this;
        }

        Scenario vaccination(VaccinationStatus status, Integer changedDaysAgo) {
            this.vaccinationStatus = status;
            this.vaccinationStatusChangedOn = changedDaysAgo == null ? null : BIRTH_DATE.plusDays(ageDays - changedDaysAgo);
            return this;
        }

        Scenario promptCount(int promptCount) {
            this.promptCount = promptCount;
            return this;
        }

        Scenario progress(String code, ProgressState state) {
            progress.put(code, state);
            return this;
        }

        PlannerInput build() {
            return new PlannerInput(BIRTH_DATE.plusDays(ageDays), BIRTH_DATE, size, petCreatedOn,
                    vaccinationStatus, vaccinationStatusChangedOn, promptCount, CATALOGUE, progress);
        }
    }
}
