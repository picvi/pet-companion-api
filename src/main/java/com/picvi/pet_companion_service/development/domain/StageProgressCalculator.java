package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.util.List;
import java.util.Map;

public class StageProgressCalculator {

    public StageProgress calculate(List<ActivityDefinition> activities, Map<String, ProgressState> progress,
                                   StageInfo stage, VaccinationStatus vaccinationStatus) {
        List<ActivityDefinition> counted = activities.stream()
                .filter(activity -> !activity.isRecurring())
                .filter(stage::overlaps)
                .filter(activity -> !ActivityEligibility.isLocked(activity, vaccinationStatus))
                .filter(activity -> statusOf(activity, progress) != ProgressStatus.NOT_POSSIBLE)
                .toList();
        int completed = (int) counted.stream()
                .filter(activity -> statusOf(activity, progress) == ProgressStatus.COMPLETED)
                .count();
        return new StageProgress(completed, counted.size());
    }

    private static ProgressStatus statusOf(ActivityDefinition activity, Map<String, ProgressState> progress) {
        return progress.getOrDefault(activity.code(), ProgressState.notStarted()).status();
    }
}
