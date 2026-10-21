package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.DevelopmentStage;

public record StageDto(DevelopmentStage code, int startWeek, Integer endWeek, Integer weeksLeft) {
}
