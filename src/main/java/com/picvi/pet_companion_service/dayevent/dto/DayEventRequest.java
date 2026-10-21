package com.picvi.pet_companion_service.dayevent.dto;

import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DayEventRequest(@NotNull DayEventType type,
                              String title,
                              @NotNull LocalDate localDate,
                              @NotNull @Min(0) @Max(1439) Integer startMinute,
                              @NotNull @Min(15) @Max(720) Integer durationMinutes,
                              @NotNull Recurrence recurrence,
                              @NotNull Boolean planAround,
                              LocalDate date) {
}
