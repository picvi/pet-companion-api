package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TodayPlanner {
    private static final int WELCOME_DAYS = 2;
    private static final int STAGE_INTRO_DAYS = 2;
    private static final int MAX_PLAN_ITEMS = 3;
    private static final int WORRIED_LOOKBACK_DAYS = 6;
    private static final int WORRIED_MINIMUM_SCARED_ACTIVITIES = 2;
    private static final int WORRIED_CARD_DAYS_AFTER_LAST_SCARED = 1;
    private static final Set<DevelopmentStage> SOCIALISATION_WINDOW_STAGES =
            Set.of(DevelopmentStage.HOMECOMING, DevelopmentStage.FINDING_CONFIDENCE);
    private static final Set<DevelopmentStage> SOCIALISATION_PLAN_STAGES =
            Set.of(DevelopmentStage.HOMECOMING, DevelopmentStage.FINDING_CONFIDENCE, DevelopmentStage.TEETHING);
    private static final Comparator<ActivityDefinition> EASIEST_FIRST = Comparator
            .comparingInt(ActivityDefinition::difficulty)
            .thenComparingInt(ActivityDefinition::sortOrder)
            .thenComparing(ActivityDefinition::code);
    private static final Comparator<ActivityDefinition> BY_SORT_ORDER = Comparator
            .comparingInt(ActivityDefinition::sortOrder)
            .thenComparing(ActivityDefinition::code);

    private final LifeStageCalculator lifeStageCalculator;
    private final StageProgressCalculator stageProgressCalculator;
    private final VaccinationPrompt vaccinationPrompt;

    public TodayPlan plan(PlannerInput input) {
        StageInfo stage = lifeStageCalculator.calculate(input.birthDate(), input.size(), input.date());
        Map<String, ProgressState> snapshot = progressAsOfStartOfDay(input);
        ActivityRotation rotation = new ActivityRotation(input, snapshot);

        Focus focus = chooseFocus(input, stage, snapshot, rotation);
        List<PlanItem> plan = buildPlan(input, stage, focus, snapshot, rotation);
        StageProgress stageProgress = stageProgressCalculator.calculate(
                input.activities(), input.progress(), stage, input.vaccinationStatus());
        boolean vaccinationPromptDue = vaccinationPrompt.isDue(
                input.vaccinationStatus(), input.vaccinationPromptCount(), stage.ageWeeks());

        return new TodayPlan(stage, stageProgress, focus, plan, showWorriedCard(input), vaccinationPromptDue);
    }

    private Map<String, ProgressState> progressAsOfStartOfDay(PlannerInput input) {
        return input.progress().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().asOfStartOf(input.date())));
    }

    private Focus chooseFocus(PlannerInput input, StageInfo stage, Map<String, ProgressState> snapshot,
                              ActivityRotation rotation) {
        if (ChronoUnit.DAYS.between(input.petCreatedOn(), input.date()) < WELCOME_DAYS) {
            return focusWithoutActivity(FocusType.WELCOME, FocusReason.ofStage(FocusReasonCode.WELCOME, stage));
        }
        if (stage.stage() != DevelopmentStage.HOMECOMING && stage.daysInStage() < STAGE_INTRO_DAYS) {
            return focusWithoutActivity(FocusType.STAGE_INTRO, FocusReason.ofStage(FocusReasonCode.STAGE_INTRO, stage));
        }

        Optional<ActivityDefinition> newlyUnlocked = newlyUnlockedActivity(input, stage, snapshot);
        if (newlyUnlocked.isPresent()) {
            return focusOn(input, FocusType.NEWLY_UNLOCKED, newlyUnlocked.get(),
                    FocusReason.ofStage(FocusReasonCode.NEWLY_UNLOCKED, stage));
        }

        if (SOCIALISATION_WINDOW_STAGES.contains(stage.stage())) {
            Optional<ActivityDefinition> socialisation = rotation.pickWithoutRepeat(ActivityKind.SOCIALIZATION);
            if (socialisation.isPresent()) {
                return activityFocus(input, stage, socialisation.get(), FocusReasonCode.SOCIALISATION_WINDOW);
            }
        }

        Optional<ActivityDefinition> skill = rotation.pickWithoutRepeat(ActivityKind.SKILL);
        if (skill.isPresent()) {
            return activityFocus(input, stage, skill.get(), FocusReasonCode.SKILL_PRACTICE);
        }

        return focusWithoutActivity(FocusType.STAGE_TIP, FocusReason.stageTip(stage.ageDays()));
    }

    private Optional<ActivityDefinition> newlyUnlockedActivity(PlannerInput input, StageInfo stage,
                                                              Map<String, ProgressState> snapshot) {
        boolean seriesCompletedYesterday = input.vaccinationStatus() == VaccinationStatus.SERIES_COMPLETE
                && input.date().minusDays(1).equals(input.vaccinationStatusChangedOn());
        if (!seriesCompletedYesterday) {
            return Optional.empty();
        }
        return input.activities().stream()
                .filter(activity -> activity.kind() == ActivityKind.SOCIALIZATION)
                .filter(activity -> activity.vaccinationRequirement() == VaccinationRequirement.SERIES_COMPLETE)
                .filter(activity -> ActivityEligibility.isSuggestable(activity, stateIn(snapshot, activity),
                        input.vaccinationStatus(), stage.ageWeeks(), input.date()))
                .min(EASIEST_FIRST);
    }

    private Focus activityFocus(PlannerInput input, StageInfo stage, ActivityDefinition activity, FocusReasonCode code) {
        List<ActivityDefinition> sameCategory = input.activities().stream()
                .filter(other -> other.category() == activity.category())
                .filter(other -> !other.isRecurring())
                .filter(other -> other.coversAgeWeeks(stage.ageWeeks()))
                .filter(other -> !ActivityEligibility.isLocked(other, input.vaccinationStatus()))
                .filter(other -> input.progressOf(other.code()).status() != ProgressStatus.NOT_POSSIBLE)
                .toList();
        int completed = (int) sameCategory.stream()
                .filter(other -> input.progressOf(other.code()).status() == ProgressStatus.COMPLETED)
                .count();
        return focusOn(input, FocusType.ACTIVITY, activity,
                FocusReason.ofCategory(code, stage, completed, sameCategory.size()));
    }

    private Focus focusOn(PlannerInput input, FocusType type, ActivityDefinition activity, FocusReason reason) {
        return new Focus(type, activity, reason, isDoneToday(input, activity));
    }

    private Focus focusWithoutActivity(FocusType type, FocusReason reason) {
        return new Focus(type, null, reason, false);
    }

    private List<PlanItem> buildPlan(PlannerInput input, StageInfo stage, Focus focus,
                                     Map<String, ProgressState> snapshot, ActivityRotation rotation) {
        List<PlanItem> candidates = focus.type() == FocusType.WELCOME
                ? homeBasics(input, stage, snapshot)
                : otherKindAndRecurring(input, stage, focus, snapshot, rotation);

        Map<String, PlanItem> uniqueByCode = new LinkedHashMap<>();
        candidates.stream()
                .filter(item -> focus.activity() == null || !item.activity().code().equals(focus.activity().code()))
                .forEach(item -> uniqueByCode.putIfAbsent(item.activity().code(), item));
        return uniqueByCode.values().stream().limit(MAX_PLAN_ITEMS).toList();
    }

    private List<PlanItem> homeBasics(PlannerInput input, StageInfo stage, Map<String, ProgressState> snapshot) {
        return input.activities().stream()
                .filter(activity -> activity.category() == ActivityCategory.HOME_LIFE)
                .filter(activity -> !activity.isRecurring())
                .filter(activity -> ActivityEligibility.isSuggestable(activity, stateIn(snapshot, activity),
                        input.vaccinationStatus(), stage.ageWeeks(), input.date()))
                .sorted(BY_SORT_ORDER)
                .limit(MAX_PLAN_ITEMS)
                .map(activity -> planItem(input, activity, PlanReason.HOME_BASICS))
                .toList();
    }

    private List<PlanItem> otherKindAndRecurring(PlannerInput input, StageInfo stage, Focus focus,
                                                 Map<String, ProgressState> snapshot, ActivityRotation rotation) {
        List<PlanItem> items = new ArrayList<>();
        otherKindActivity(stage, focus, rotation)
                .ifPresent(activity -> items.add(planItem(input, activity, PlanReason.OTHER_KIND)));
        input.activities().stream()
                .filter(ActivityDefinition::isRecurring)
                .filter(activity -> activity.coversAgeWeeks(stage.ageWeeks()))
                .filter(activity -> !ActivityEligibility.isLocked(activity, input.vaccinationStatus()))
                .filter(activity -> stateIn(snapshot, activity).status() != ProgressStatus.NOT_POSSIBLE)
                .filter(activity -> !ActivityEligibility.isSnoozed(stateIn(snapshot, activity), input.date()))
                .filter(activity -> ActivityEligibility.isRecurringDue(activity, stateIn(snapshot, activity), input.date()))
                .sorted(BY_SORT_ORDER)
                .forEach(activity -> items.add(planItem(input, activity, PlanReason.RECURRING_DUE)));
        return items;
    }

    private Optional<ActivityDefinition> otherKindActivity(StageInfo stage, Focus focus, ActivityRotation rotation) {
        boolean focusIsSocialisation = focus.activity() != null && focus.activity().kind() == ActivityKind.SOCIALIZATION;
        if (focusIsSocialisation || !SOCIALISATION_PLAN_STAGES.contains(stage.stage())) {
            return rotation.pickWithoutRepeat(ActivityKind.SKILL);
        }
        return rotation.pickWithoutRepeat(ActivityKind.SOCIALIZATION);
    }

    private PlanItem planItem(PlannerInput input, ActivityDefinition activity, PlanReason reason) {
        return new PlanItem(activity, reason, isDoneToday(input, activity));
    }

    private boolean showWorriedCard(PlannerInput input) {
        LocalDate oldestRelevantDay = input.date().minusDays(WORRIED_LOOKBACK_DAYS);
        List<LocalDate> recentScaredDays = input.activities().stream()
                .filter(activity -> activity.kind() == ActivityKind.SOCIALIZATION)
                .map(activity -> input.progressOf(activity.code()))
                .filter(state -> state.lastOutcome() == Outcome.SCARED)
                .map(ProgressState::lastAttemptOn)
                .filter(day -> !day.isBefore(oldestRelevantDay) && !day.isAfter(input.date()))
                .toList();
        if (recentScaredDays.size() < WORRIED_MINIMUM_SCARED_ACTIVITIES) {
            return false;
        }
        LocalDate latestScaredDay = recentScaredDays.stream().max(Comparator.naturalOrder()).orElseThrow();
        return !latestScaredDay.isBefore(input.date().minusDays(WORRIED_CARD_DAYS_AFTER_LAST_SCARED));
    }

    private static boolean isDoneToday(PlannerInput input, ActivityDefinition activity) {
        return input.date().equals(input.progressOf(activity.code()).lastAttemptOn());
    }

    private static ProgressState stateIn(Map<String, ProgressState> progress, ActivityDefinition activity) {
        return progress.getOrDefault(activity.code(), ProgressState.notStarted());
    }
}
