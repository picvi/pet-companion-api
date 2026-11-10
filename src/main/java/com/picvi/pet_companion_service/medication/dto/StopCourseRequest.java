package com.picvi.pet_companion_service.medication.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record StopCourseRequest(@NotNull LocalDate date, @NotNull @Min(0) @Max(1439) Integer minuteOfDay) {
}
