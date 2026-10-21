package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.Outcome;

import java.time.LocalDate;
import java.util.UUID;

public record AloneSessionDto(UUID id, LocalDate performedOn, int durationSeconds, Outcome reaction) {
}
