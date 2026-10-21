package com.picvi.pet_companion_service.care.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LogCareEventRequest(@NotBlank String itemCode, @NotNull LocalDate performedOn, LocalDate date) {
}
