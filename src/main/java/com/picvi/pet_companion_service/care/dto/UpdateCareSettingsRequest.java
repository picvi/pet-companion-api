package com.picvi.pet_companion_service.care.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpdateCareSettingsRequest(@NotNull Boolean remindersEnabled,
                                        LocalDate pausedUntil,
                                        Integer intervalDays,
                                        LocalDate date) {
}
