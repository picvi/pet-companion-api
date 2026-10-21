package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record PlannerInput(LocalDate date, LocalDate birthDate, SizeCategory size, LocalDate petCreatedOn,
                           VaccinationStatus vaccinationStatus, LocalDate vaccinationStatusChangedOn,
                           int vaccinationPromptCount,
                           List<ActivityDefinition> activities, Map<String, ProgressState> progress) {

    public ProgressState progressOf(String activityCode) {
        return progress.getOrDefault(activityCode, ProgressState.notStarted());
    }
}
