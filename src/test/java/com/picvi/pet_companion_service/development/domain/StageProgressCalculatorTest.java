package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StageProgressCalculatorTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate DAY = BIRTH_DATE.plusWeeks(12);

    private final StageProgressCalculator calculator = new StageProgressCalculator();
    private final StageInfo findingConfidence =
            new LifeStageCalculator().calculate(BIRTH_DATE, SizeCategory.MEDIUM, DAY);

    @Test
    void completedActivitiesAreCounted() {
        List<ActivityDefinition> activities = List.of(activity("A", 8, 16), activity("B", 8, 16), activity("C", 8, 16));

        StageProgress progress = calculate(activities, Map.of("A", status(ProgressStatus.COMPLETED)),
                VaccinationStatus.UNKNOWN);

        assertThat(progress).isEqualTo(new StageProgress(1, 3));
    }

    @Test
    void lockedActivitiesAreExcluded() {
        List<ActivityDefinition> activities = List.of(
                activity("A", 8, 16),
                new ActivityDefinition("LOCKED", ActivityKind.SOCIALIZATION, ActivityCategory.PLACES,
                        8, 16, 2, VaccinationRequirement.SERIES_COMPLETE, null, 10));

        assertThat(calculate(activities, Map.of(), VaccinationStatus.FIRST_DOSE)).isEqualTo(new StageProgress(0, 1));
        assertThat(calculate(activities, Map.of(), VaccinationStatus.SERIES_COMPLETE)).isEqualTo(new StageProgress(0, 2));
    }

    @Test
    void notPossibleActivitiesAreExcluded() {
        List<ActivityDefinition> activities = List.of(activity("A", 8, 16), activity("B", 8, 16));

        StageProgress progress = calculate(activities, Map.of("B", status(ProgressStatus.NOT_POSSIBLE)),
                VaccinationStatus.UNKNOWN);

        assertThat(progress).isEqualTo(new StageProgress(0, 1));
    }

    @Test
    void recurringActivitiesAreExcluded() {
        List<ActivityDefinition> activities = List.of(
                activity("A", 8, 16),
                new ActivityDefinition("WEEKLY", ActivityKind.SKILL, ActivityCategory.CARE,
                        8, 52, 1, VaccinationRequirement.NONE, 7, 10));

        assertThat(calculate(activities, Map.of(), VaccinationStatus.UNKNOWN)).isEqualTo(new StageProgress(0, 1));
    }

    @Test
    void activitiesOutsideTheStageWindowAreExcluded() {
        List<ActivityDefinition> activities = List.of(
                activity("BEFORE", 4, 10),
                activity("OVERLAPS_START", 4, 11),
                activity("OVERLAPS_END", 15, 20),
                activity("AFTER", 16, 20));

        StageProgress progress = calculate(activities, Map.of(), VaccinationStatus.UNKNOWN);

        assertThat(progress).isEqualTo(new StageProgress(0, 2));
    }

    @Test
    void stageWithoutEndCountsEverythingThatEndsAfterItStarts() {
        StageInfo youngAdult = new LifeStageCalculator().calculate(BIRTH_DATE, SizeCategory.SMALL, BIRTH_DATE.plusWeeks(60));

        StageProgress progress = calculator.calculate(
                List.of(activity("OLD", 0, 52), activity("CURRENT", 40, 200)), Map.of(), youngAdult,
                VaccinationStatus.UNKNOWN);

        assertThat(progress).isEqualTo(new StageProgress(0, 1));
    }

    private StageProgress calculate(List<ActivityDefinition> activities, Map<String, ProgressState> progress,
                                    VaccinationStatus vaccinationStatus) {
        return calculator.calculate(activities, progress, findingConfidence, vaccinationStatus);
    }

    private static ProgressState status(ProgressStatus status) {
        LocalDate completedOn = status == ProgressStatus.COMPLETED ? DAY : null;
        return new ProgressState(status, null, 1, DAY, DAY, completedOn, null);
    }

    private static ActivityDefinition activity(String code, int startWeek, int endWeek) {
        return new ActivityDefinition(code, ActivityKind.SOCIALIZATION, ActivityCategory.PEOPLE,
                startWeek, endWeek, 1, VaccinationRequirement.NONE, null, 10);
    }
}
