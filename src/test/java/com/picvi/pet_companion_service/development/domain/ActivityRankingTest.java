package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.development.ActivityCatalogueFixture;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityRankingTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);
    private static final int AGE_WEEKS = 12;

    private final ActivityRanking ranking = new ActivityRanking();
    private final Map<String, ProgressState> progress = new HashMap<>();

    @Test
    void completedActivityIsDone() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null));

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.DONE);
    }

    @Test
    void notPossibleActivityIsNotPossibleEvenWhenLocked() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.SERIES_COMPLETE, null, 1);
        progress.put("A", new ProgressState(ProgressStatus.NOT_POSSIBLE, null, 0, null, null, null, null));

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.NOT_POSSIBLE);
    }

    @Test
    void lockedActivityIsLockedEvenWhenInProgress() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.FIRST_DOSE, null, 1);
        progress.put("A", inProgress(Outcome.UNSURE, DAY.minusDays(5)));

        RankedActivity ranked = ranking.rank(List.of(activity), progress, VaccinationStatus.UNKNOWN, AGE_WEEKS, DAY).get(0);

        assertThat(ranked.section()).isEqualTo(ActivitySection.LOCKED);
    }

    @Test
    void snoozedActivityIsRestingUntilSnoozeEnds() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", new ProgressState(ProgressStatus.NOT_STARTED, null, 0, null, null, null, DAY.plusDays(3)));

        RankedActivity ranked = rankOne(activity);

        assertThat(ranked.section()).isEqualTo(ActivitySection.RESTING);
        assertThat(ranked.availableFrom()).isEqualTo(DAY.plusDays(3));
    }

    @Test
    void snoozedActivityIsOpenAgainOnTheSnoozeEndDate() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", new ProgressState(ProgressStatus.NOT_STARTED, null, 0, null, null, null, DAY));

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.OPEN);
    }

    @Test
    void unsureActivityRestsTwoDays() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", inProgress(Outcome.UNSURE, DAY));

        RankedActivity ranked = rankOne(activity);

        assertThat(ranked.section()).isEqualTo(ActivitySection.RESTING);
        assertThat(ranked.availableFrom()).isEqualTo(DAY.plusDays(2));
        assertThat(rankOneOn(activity, DAY.plusDays(2)).section()).isEqualTo(ActivitySection.ACTIVE);
    }

    @Test
    void scaredActivityRestsSevenDays() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", inProgress(Outcome.SCARED, DAY));

        RankedActivity ranked = rankOne(activity);

        assertThat(ranked.section()).isEqualTo(ActivitySection.RESTING);
        assertThat(ranked.availableFrom()).isEqualTo(DAY.plusDays(7));
        assertThat(rankOneOn(activity, DAY.plusDays(7)).section()).isEqualTo(ActivitySection.ACTIVE);
    }

    @Test
    void restingActivityUsesTheLaterOfRestAndSnooze() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.UNSURE, 1, DAY, DAY, null,
                DAY.plusDays(5)));

        assertThat(rankOne(activity).availableFrom()).isEqualTo(DAY.plusDays(5));
    }

    @Test
    void dueRecurringActivityIsActive() {
        ActivityDefinition activity = activity("A", 8, 52, 1, VaccinationRequirement.NONE, 7, 1);

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.ACTIVE);
    }

    @Test
    void recurringActivityNotDueIsRestingUntilNextDue() {
        ActivityDefinition activity = activity("A", 8, 52, 1, VaccinationRequirement.NONE, 7, 1);
        progress.put("A", new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.PRACTISED, 1, DAY.minusDays(2),
                DAY.minusDays(2), null, null));

        RankedActivity ranked = rankOne(activity);

        assertThat(ranked.section()).isEqualTo(ActivitySection.RESTING);
        assertThat(ranked.availableFrom()).isEqualTo(DAY.plusDays(5));
    }

    @Test
    void activityStartingLaterIsUpcoming() {
        ActivityDefinition activity = activity("A", 14, 26, 1, VaccinationRequirement.NONE, null, 1);

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.UPCOMING);
    }

    @Test
    void inProgressActivityIsActive() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        progress.put("A", inProgress(Outcome.PRACTISED, DAY.minusDays(3)));

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.ACTIVE);
    }

    @Test
    void notStartedActivityIsOpen() {
        ActivityDefinition activity = activity("A", 8, 16, 1, VaccinationRequirement.NONE, null, 1);

        assertThat(rankOne(activity).section()).isEqualTo(ActivitySection.OPEN);
    }

    @Test
    void activityWithPassedWindowStaysOpenAndRanksLast() {
        ActivityDefinition passed = activity("PASSED", 4, 10, 1, VaccinationRequirement.NONE, null, 1);
        ActivityDefinition closing = activity("CLOSING", 8, 16, 1, VaccinationRequirement.NONE, null, 2);

        List<RankedActivity> ranked = rankAll(passed, closing);

        assertThat(ranked).extracting(item -> item.activity().code()).containsExactly("CLOSING", "PASSED");
        assertThat(ranked).extracting(RankedActivity::section).containsOnly(ActivitySection.OPEN);
    }

    @Test
    void openActivitiesClosingSoonerRankFirst() {
        ActivityDefinition later = activity("LATER", 8, 26, 1, VaccinationRequirement.NONE, null, 1);
        ActivityDefinition sooner = activity("SOONER", 8, 14, 1, VaccinationRequirement.NONE, null, 2);

        assertThat(rankAll(later, sooner)).extracting(item -> item.activity().code())
                .containsExactly("SOONER", "LATER");
    }

    @Test
    void openActivitiesWithSameWindowRankEasierFirst() {
        ActivityDefinition harder = activity("HARDER", 8, 16, 3, VaccinationRequirement.NONE, null, 1);
        ActivityDefinition easier = activity("EASIER", 8, 16, 1, VaccinationRequirement.NONE, null, 2);

        assertThat(rankAll(harder, easier)).extracting(item -> item.activity().code())
                .containsExactly("EASIER", "HARDER");
    }

    @Test
    void recurringActivityRanksFirstAmongActive() {
        ActivityDefinition inProgressSkill = activity("SKILL", 8, 14, 1, VaccinationRequirement.NONE, null, 1);
        ActivityDefinition recurring = activity("WEIGH_IN", 8, 52, 1, VaccinationRequirement.NONE, 7, 2);
        progress.put("SKILL", inProgress(Outcome.PRACTISED, DAY.minusDays(3)));

        assertThat(rankAll(inProgressSkill, recurring)).extracting(item -> item.activity().code())
                .containsExactly("WEIGH_IN", "SKILL");
    }

    @Test
    void doneActivitiesShowMostRecentFirst() {
        ActivityDefinition older = activity("OLDER", 8, 16, 1, VaccinationRequirement.NONE, null, 1);
        ActivityDefinition newer = activity("NEWER", 8, 16, 1, VaccinationRequirement.NONE, null, 2);
        progress.put("OLDER", new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1,
                DAY.minusDays(9), DAY.minusDays(9), DAY.minusDays(9), null));
        progress.put("NEWER", new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1,
                DAY.minusDays(1), DAY.minusDays(1), DAY.minusDays(1), null));

        assertThat(rankAll(older, newer)).extracting(item -> item.activity().code())
                .containsExactly("NEWER", "OLDER");
    }

    @Test
    void lockedActivitiesRankFirstDoseBeforeSeriesComplete() {
        ActivityDefinition series = activity("SERIES", 8, 16, 1, VaccinationRequirement.SERIES_COMPLETE, null, 1);
        ActivityDefinition firstDose = activity("FIRST", 8, 16, 1, VaccinationRequirement.FIRST_DOSE, null, 2);

        List<RankedActivity> ranked = ranking.rank(List.of(series, firstDose), progress, VaccinationStatus.UNKNOWN,
                AGE_WEEKS, DAY);

        assertThat(ranked).extracting(item -> item.activity().code()).containsExactly("FIRST", "SERIES");
    }

    @Test
    void sectionsFollowDisplayOrder() {
        List<ActivityDefinition> activities = new ArrayList<>(List.of(
                activity("ACTIVE", 8, 16, 1, VaccinationRequirement.NONE, null, 1),
                activity("OPEN", 8, 16, 1, VaccinationRequirement.NONE, null, 2),
                activity("RESTING", 8, 16, 1, VaccinationRequirement.NONE, null, 3),
                activity("UPCOMING", 14, 26, 1, VaccinationRequirement.NONE, null, 4),
                activity("LOCKED", 8, 16, 1, VaccinationRequirement.SERIES_COMPLETE, null, 5),
                activity("DONE", 8, 16, 1, VaccinationRequirement.NONE, null, 6),
                activity("NOT_POSSIBLE", 8, 16, 1, VaccinationRequirement.NONE, null, 7)));
        progress.put("ACTIVE", inProgress(Outcome.PRACTISED, DAY.minusDays(3)));
        progress.put("RESTING", inProgress(Outcome.SCARED, DAY));
        progress.put("DONE", new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null));
        progress.put("NOT_POSSIBLE", new ProgressState(ProgressStatus.NOT_POSSIBLE, null, 0, null, null, null, null));
        Collections.shuffle(activities, new Random(7));

        List<RankedActivity> ranked = ranking.rank(activities, progress, VaccinationStatus.FIRST_DOSE, AGE_WEEKS, DAY);

        assertThat(ranked).extracting(item -> item.activity().code()).containsExactly(
                "ACTIVE", "OPEN", "RESTING", "UPCOMING", "LOCKED", "DONE", "NOT_POSSIBLE");
        assertThat(ranked).extracting(RankedActivity::section).containsExactly(ActivitySection.values());
    }

    @Test
    void ranksAreConsecutiveFromZero() {
        List<RankedActivity> ranked = ranking.rank(ActivityCatalogueFixture.load(), progress,
                VaccinationStatus.FIRST_DOSE, AGE_WEEKS, DAY);

        assertThat(ranked).extracting(RankedActivity::rank)
                .containsExactlyElementsOf(IntStream.range(0, ranked.size()).boxed().toList());
    }

    @Test
    void everyActivityIsReturnedOnce() {
        List<ActivityDefinition> catalogue = ActivityCatalogueFixture.load();

        List<RankedActivity> ranked = ranking.rank(catalogue, progress, VaccinationStatus.UNKNOWN, AGE_WEEKS, DAY);

        assertThat(ranked).hasSameSizeAs(catalogue);
        assertThat(ranked).extracting(item -> item.activity().code()).doesNotHaveDuplicates();
    }

    private RankedActivity rankOne(ActivityDefinition activity) {
        return rankOneOn(activity, DAY);
    }

    private RankedActivity rankOneOn(ActivityDefinition activity, LocalDate date) {
        return ranking.rank(List.of(activity), progress, VaccinationStatus.SERIES_COMPLETE, AGE_WEEKS, date).get(0);
    }

    private List<RankedActivity> rankAll(ActivityDefinition... activities) {
        return ranking.rank(List.of(activities), progress, VaccinationStatus.SERIES_COMPLETE, AGE_WEEKS, DAY);
    }

    private static ProgressState inProgress(Outcome outcome, LocalDate lastAttemptOn) {
        return new ProgressState(ProgressStatus.IN_PROGRESS, outcome, 1, lastAttemptOn, lastAttemptOn, null, null);
    }

    private static ActivityDefinition activity(String code, int startWeek, int endWeek, int difficulty,
                                               VaccinationRequirement requirement, Integer repeatEveryDays,
                                               int sortOrder) {
        return new ActivityDefinition(code, ActivityKind.SKILL, ActivityCategory.HOME_LIFE, startWeek, endWeek,
                difficulty, requirement, repeatEveryDays, sortOrder);
    }
}
