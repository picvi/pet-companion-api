package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class LifeStageCalculator {
    private static final int SOCIALISATION_WINDOW_END_WEEK = 16;
    private static final Set<DevelopmentStage> SOCIALISATION_WINDOW_STAGES =
            Set.of(DevelopmentStage.HOMECOMING, DevelopmentStage.FINDING_CONFIDENCE);

    public StageInfo calculate(LocalDate birthDate, SizeCategory size, LocalDate date) {
        if (date.isBefore(birthDate)) {
            throw new IllegalArgumentException("Date " + date + " is before birth date " + birthDate);
        }

        int ageDays = (int) ChronoUnit.DAYS.between(birthDate, date);
        int ageWeeks = ageDays / 7;
        DevelopmentStage stage = DevelopmentStage.atAgeWeeks(ageWeeks, size);
        int stageStartWeek = stage.startWeek(size);
        Integer stageEndWeek = stage.endWeek(size);
        DevelopmentStage nextStage = stage.next().orElse(null);

        return new StageInfo(
                stage,
                ageDays,
                ageWeeks,
                stageStartWeek,
                stageEndWeek,
                ageDays - stageStartWeek * 7,
                stageEndWeek == null ? null : stageEndWeek - ageWeeks,
                nextStage,
                nextStage == null ? null : nextStage.startWeek(size),
                SOCIALISATION_WINDOW_STAGES.contains(stage)
                        ? Math.max(0, SOCIALISATION_WINDOW_END_WEEK - ageWeeks)
                        : null
        );
    }
}
