package com.picvi.pet_companion_service.medication.dto;

import java.time.LocalDate;
import java.util.UUID;

/** A planned dose; {@code logId} is set once it was given. */
public record PlannedDoseDto(LocalDate on, int minute, UUID logId) {
}
