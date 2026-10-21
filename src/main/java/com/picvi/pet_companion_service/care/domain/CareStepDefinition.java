package com.picvi.pet_companion_service.care.domain;

public record CareStepDefinition(StepKind kind,
                                 Integer minAgeWeeks,
                                 Integer maxAgeWeeks,
                                 Integer gapDays,
                                 Integer seasonStartMonth,
                                 Integer seasonEndMonth) {

    public boolean isOpenEndedRepeat() {
        return this.kind == StepKind.REPEAT && this.maxAgeWeeks == null;
    }

    public boolean hasSeason() {
        return this.seasonStartMonth != null;
    }
}
