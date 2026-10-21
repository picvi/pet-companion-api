package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.DevelopmentStage;

public record StageWindowDto(DevelopmentStage code, int startWeek, Integer endWeek) {
}
