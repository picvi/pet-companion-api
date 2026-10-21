package com.picvi.pet_companion_service.care.domain;

import java.util.List;
import java.util.Optional;

public record CareItemDefinition(String code,
                                 CareCategory category,
                                 CareRequirement requirement,
                                 boolean intervalEditable,
                                 Integer legalMinAgeMonths,
                                 Integer legalWindowDays,
                                 Integer legalRepeatMonths,
                                 List<CareStepDefinition> steps) {

    public boolean hasLegalDeadline() {
        return this.legalMinAgeMonths != null;
    }

    public List<CareStepDefinition> doseSteps() {
        return this.steps.stream().filter(step -> step.kind() != StepKind.REPEAT).toList();
    }

    public List<CareStepDefinition> repeatSteps() {
        return this.steps.stream().filter(step -> step.kind() == StepKind.REPEAT).toList();
    }

    public int seriesTotal() {
        return (int) this.steps.stream().filter(step -> step.kind() == StepKind.SERIES).count();
    }

    public Optional<CareStepDefinition> openEndedRepeat() {
        return this.repeatSteps().stream().filter(CareStepDefinition::isOpenEndedRepeat).findFirst();
    }
}
