package com.picvi.pet_companion_service.brief.dto;

import com.picvi.pet_companion_service.brief.domain.Concern;

import java.time.LocalDate;
import java.util.List;

public record BriefOptionsResponse(LocalDate date, PeriodDto defaultPeriod, List<ConcernOptionDto> concerns) {

    public record PeriodDto(LocalDate from, LocalDate to) {
    }

    public record ConcernOptionDto(Concern code, int count, LocalDate firstSeen, LocalDate lastSeen, LocalDate defaultFrom) {
    }
}
