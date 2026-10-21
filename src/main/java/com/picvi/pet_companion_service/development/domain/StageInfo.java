package com.picvi.pet_companion_service.development.domain;

public record StageInfo(DevelopmentStage stage, int ageDays, int ageWeeks, int stageStartWeek,
                        Integer stageEndWeek, int daysInStage, Integer weeksLeftInStage,
                        DevelopmentStage nextStage, Integer nextStageStartWeek, Integer weeksLeftInWindow) {

    public boolean overlaps(ActivityDefinition activity) {
        boolean startsBeforeStageEnds = stageEndWeek == null || activity.startWeek() < stageEndWeek;
        return startsBeforeStageEnds && activity.endWeek() > stageStartWeek;
    }
}
