package com.picvi.pet_companion_service.pet.dto;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record PetProfile(UUID id,
                         String name,
                         LocalDate birthDate,
                         SizeCategory sizeCategory,
                         LocalDate createdOn,
                         VaccinationStatus vaccinationStatus,
                         LocalDate vaccinationStatusChangedOn,
                         int vaccinationPromptCount) {
}
