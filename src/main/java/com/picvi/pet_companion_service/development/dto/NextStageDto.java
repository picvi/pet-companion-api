package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.DevelopmentStage;

public record NextStageDto(DevelopmentStage code, int startsAtWeek) {
}
