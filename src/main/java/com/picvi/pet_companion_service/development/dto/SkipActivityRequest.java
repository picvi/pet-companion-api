package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.SkipReason;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SkipActivityRequest(LocalDate date, @NotNull SkipReason reason) {
}
