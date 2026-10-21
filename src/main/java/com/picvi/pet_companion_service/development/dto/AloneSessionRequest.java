package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.Outcome;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AloneSessionRequest(LocalDate date,
                                  @Min(1) @Max(14400) int durationSeconds,
                                  @NotNull Outcome reaction) {
}
