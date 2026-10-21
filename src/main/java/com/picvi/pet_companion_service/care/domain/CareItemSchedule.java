package com.picvi.pet_companion_service.care.domain;

import java.time.LocalDate;
import java.util.List;

public record CareItemSchedule(String code,
                               CareCategory category,
                               CareRequirement requirement,
                               CareStatus status,
                               CarePhase phase,
                               Integer doseNumber,
                               Integer seriesTotal,
                               LocalDate availableFrom,
                               LocalDate dueOn,
                               LocalDate legalDeadline,
                               LocalDate lastPerformedOn,
                               List<LocalDate> remindOn,
                               boolean remindersEnabled,
                               LocalDate pausedUntil,
                               boolean intervalEditable,
                               Integer defaultIntervalDays,
                               Integer customIntervalDays) {
}
