package com.picvi.pet_companion_service.development.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActivityRotation {
    private static final Comparator<ActivityDefinition> EASIEST_FIRST = Comparator
            .comparingInt(ActivityDefinition::difficulty)
            .thenComparingInt(ActivityDefinition::endWeek)
            .thenComparingInt(ActivityDefinition::sortOrder)
            .thenComparing(ActivityDefinition::code);

    private final PlannerInput input;
    private final Map<String, ProgressState> snapshot;

    public ActivityRotation(PlannerInput input, Map<String, ProgressState> snapshot) {
        this.input = input;
        this.snapshot = snapshot;
    }

    public Optional<ActivityDefinition> pickWithoutRepeat(ActivityKind kind) {
        LocalDate firstDay = earlier(input.date(), later(input.petCreatedOn(), input.birthDate()));
        Optional<ActivityDefinition> pick = Optional.empty();
        for (LocalDate day = firstDay; !day.isAfter(input.date()); day = day.plusDays(1)) {
            String previousCode = pick.map(ActivityDefinition::code).orElse(null);
            pick = pickForDay(kind, day, previousCode);
        }
        return pick;
    }

    private Optional<ActivityDefinition> pickForDay(ActivityKind kind, LocalDate day, String excludedCode) {
        int ageDays = (int) ChronoUnit.DAYS.between(input.birthDate(), day);
        int ageWeeks = ageDays / 7;
        List<ActivityDefinition> candidates = input.activities().stream()
                .filter(activity -> activity.kind() == kind)
                .filter(activity -> !activity.isRecurring())
                .filter(activity -> !activity.code().equals(excludedCode))
                .filter(activity -> ActivityEligibility.isSuggestable(activity, stateOf(activity), input.vaccinationStatus(), ageWeeks, day))
                .toList();

        List<ActivityCategory> categories = ActivityCategory.rotationOrder(kind).stream()
                .filter(category -> candidates.stream().anyMatch(activity -> activity.category() == category))
                .toList();
        if (categories.isEmpty()) {
            return Optional.empty();
        }

        ActivityCategory category = categories.get(ageDays % categories.size());
        return candidates.stream()
                .filter(activity -> activity.category() == category)
                .min(EASIEST_FIRST);
    }

    private ProgressState stateOf(ActivityDefinition activity) {
        return snapshot.getOrDefault(activity.code(), ProgressState.notStarted());
    }

    private static LocalDate earlier(LocalDate first, LocalDate second) {
        return first.isBefore(second) ? first : second;
    }

    private static LocalDate later(LocalDate first, LocalDate second) {
        return first.isAfter(second) ? first : second;
    }
}
