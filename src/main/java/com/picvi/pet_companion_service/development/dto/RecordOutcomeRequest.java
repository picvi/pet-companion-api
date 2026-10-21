package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.Outcome;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RecordOutcomeRequest(LocalDate date, @NotNull Outcome outcome) {
}
