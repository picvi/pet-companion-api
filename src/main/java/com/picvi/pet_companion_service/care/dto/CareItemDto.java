package com.picvi.pet_companion_service.care.dto;

import com.picvi.pet_companion_service.care.domain.CareCategory;
import com.picvi.pet_companion_service.care.domain.CareItemSchedule;
import com.picvi.pet_companion_service.care.domain.CarePhase;
import com.picvi.pet_companion_service.care.domain.CareRequirement;
import com.picvi.pet_companion_service.care.domain.CareStatus;

import java.time.LocalDate;
import java.util.List;

public record CareItemDto(String code,
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

    public static CareItemDto of(CareItemSchedule schedule) {
        return new CareItemDto(
                schedule.code(),
                schedule.category(),
                schedule.requirement(),
                schedule.status(),
                schedule.phase(),
                schedule.doseNumber(),
                schedule.seriesTotal(),
                schedule.availableFrom(),
                schedule.dueOn(),
                schedule.legalDeadline(),
                schedule.lastPerformedOn(),
                schedule.remindOn(),
                schedule.remindersEnabled(),
                schedule.pausedUntil(),
                schedule.intervalEditable(),
                schedule.defaultIntervalDays(),
                schedule.customIntervalDays()
        );
    }
}
