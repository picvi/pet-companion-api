package com.picvi.pet_companion_service.lostdog.dto;

import com.picvi.pet_companion_service.lostdog.domain.LostStepCode;

import java.time.Instant;

public record LostReportStepDto(LostStepCode code, Instant doneAt) {}
