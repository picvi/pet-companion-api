package com.picvi.pet_companion_service.development.domain;

public record ActivityDefinition(String code, ActivityKind kind, ActivityCategory category,
                                 int startWeek, int endWeek, int difficulty,
                                 VaccinationRequirement vaccinationRequirement,
                                 Integer repeatEveryDays, int sortOrder) {

    public boolean isRecurring() {
        return repeatEveryDays != null;
    }

    public boolean coversAgeWeeks(int ageWeeks) {
        return startWeek <= ageWeeks && ageWeeks < endWeek;
    }
}
