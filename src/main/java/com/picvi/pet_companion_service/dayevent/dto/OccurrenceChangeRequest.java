package com.picvi.pet_companion_service.dayevent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record OccurrenceChangeRequest(@NotNull Boolean skipped,
                                      @Min(0) @Max(1439) Integer startMinute,
                                      LocalDate date) {
}
