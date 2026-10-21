package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ActivityRanking {

    public List<RankedActivity> rank(List<ActivityDefinition> activities, Map<String, ProgressState> progress,
                                     VaccinationStatus vaccinationStatus, int ageWeeks, LocalDate date) {
        List<Classified> sorted = activities.stream()
                .map(activity -> classify(activity,
                        progress.getOrDefault(activity.code(), ProgressState.notStarted()),
                        vaccinationStatus, ageWeeks, date))
                .sorted(displayOrder(ageWeeks))
                .toList();

        return IntStream.range(0, sorted.size())
                .mapToObj(index -> sorted.get(index).withRank(index))
                .toList();
    }

    private Classified classify(ActivityDefinition activity, ProgressState state, VaccinationStatus vaccinationStatus,
                                int ageWeeks, LocalDate date) {
        if (state.status() == ProgressStatus.COMPLETED) {
            return new Classified(activity, state, ActivitySection.DONE, null);
        }
        if (state.status() == ProgressStatus.NOT_POSSIBLE) {
            return new Classified(activity, state, ActivitySection.NOT_POSSIBLE, null);
        }
        if (ActivityEligibility.isLocked(activity, vaccinationStatus)) {
            return new Classified(activity, state, ActivitySection.LOCKED, null);
        }
        if (ActivityEligibility.isSnoozed(state, date) || ActivityEligibility.isResting(state, date)) {
            return new Classified(activity, state, ActivitySection.RESTING, laterOf(state, date));
        }
        if (activity.isRecurring()) {
            return ActivityEligibility.isRecurringDue(activity, state, date)
                    ? new Classified(activity, state, ActivitySection.ACTIVE, null)
                    : new Classified(activity, state, ActivitySection.RESTING,
                    state.lastAttemptOn().plusDays(activity.repeatEveryDays()));
        }
        if (ageWeeks < activity.startWeek()) {
            return new Classified(activity, state, ActivitySection.UPCOMING, null);
        }
        if (state.status() == ProgressStatus.IN_PROGRESS) {
            return new Classified(activity, state, ActivitySection.ACTIVE, null);
        }
        return new Classified(activity, state, ActivitySection.OPEN, null);
    }

    private LocalDate laterOf(ProgressState state, LocalDate date) {
        LocalDate snoozedUntil = ActivityEligibility.isSnoozed(state, date) ? state.snoozedUntil() : null;
        LocalDate restingUntil = ActivityEligibility.isResting(state, date)
                ? ActivityEligibility.restingUntil(state) : null;
        return Stream.of(snoozedUntil, restingUntil)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElseThrow();
    }

    private Comparator<Classified> displayOrder(int ageWeeks) {
        return (first, second) -> {
            if (first.section() != second.section()) {
                return first.section().compareTo(second.section());
            }
            return orderWithinSection(first.section(), ageWeeks).compare(first, second);
        };
    }

    private Comparator<Classified> orderWithinSection(ActivitySection section, int ageWeeks) {
        Comparator<Classified> byWindow = Comparator.comparingInt(item -> windowKey(item.activity(), ageWeeks));
        Comparator<Classified> byDifficulty = Comparator.comparingInt(item -> item.activity().difficulty());
        Comparator<Classified> bySortOrder = Comparator.comparingInt(item -> item.activity().sortOrder());
        Comparator<Classified> byCode = Comparator.comparing(item -> item.activity().code());

        return switch (section) {
            case ACTIVE -> Comparator
                    .comparing((Classified item) -> !item.activity().isRecurring())
                    .thenComparing(byWindow).thenComparing(byDifficulty).thenComparing(bySortOrder).thenComparing(byCode);
            case OPEN -> byWindow.thenComparing(byDifficulty).thenComparing(bySortOrder).thenComparing(byCode);
            case RESTING -> Comparator
                    .comparing(Classified::availableFrom)
                    .thenComparing(bySortOrder).thenComparing(byCode);
            case UPCOMING -> Comparator
                    .comparingInt((Classified item) -> item.activity().startWeek())
                    .thenComparing(byDifficulty).thenComparing(bySortOrder).thenComparing(byCode);
            case LOCKED -> Comparator
                    .comparing((Classified item) -> item.activity().vaccinationRequirement())
                    .thenComparingInt(item -> item.activity().startWeek())
                    .thenComparing(bySortOrder).thenComparing(byCode);
            case DONE -> Comparator
                    .comparing((Classified item) -> item.state().lastAttemptOn(),
                            Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
                    .thenComparing(bySortOrder).thenComparing(byCode);
            case NOT_POSSIBLE -> bySortOrder.thenComparing(byCode);
        };
    }

    private static int windowKey(ActivityDefinition activity, int ageWeeks) {
        int weeksLeft = activity.endWeek() - ageWeeks;
        return weeksLeft > 0 ? weeksLeft : Integer.MAX_VALUE;
    }

    private record Classified(ActivityDefinition activity, ProgressState state, ActivitySection section,
                              LocalDate availableFrom) {

        RankedActivity withRank(int rank) {
            return new RankedActivity(activity, state, section, rank, availableFrom);
        }
    }
}
