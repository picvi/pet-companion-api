package com.picvi.pet_companion_service.pet.dto;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateVaccinationStatusRequest(@NotNull VaccinationStatus status, LocalDate date) {
}
