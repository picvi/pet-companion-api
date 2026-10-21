package com.picvi.pet_companion_service.rhythm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record UpdateRhythmSettingsRequest(@NotNull @Min(0) @Max(1439) Integer wakeMinute,
                                          @NotNull @Min(0) @Max(1439) Integer bedMinute,
                                          @NotEmpty List<@NotNull @Min(0) @Max(1439) Integer> mealMinutes,
                                          Integer customAwakeMinutes,
                                          LocalDate date) {
}
