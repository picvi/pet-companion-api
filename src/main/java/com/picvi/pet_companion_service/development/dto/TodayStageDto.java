package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.DevelopmentStage;

public record TodayStageDto(DevelopmentStage code, Integer weeksLeft, Integer weeksLeftInWindow,
                            int completed, int total) {
}
