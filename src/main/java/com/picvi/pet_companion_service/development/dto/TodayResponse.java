package com.picvi.pet_companion_service.development.dto;

import java.time.LocalDate;
import java.util.List;

public record TodayResponse(LocalDate date, String petName, int ageWeeks, TodayStageDto stage,
                            FocusDto focus, List<PlanItemDto> plan,
                            boolean showWorriedCard, boolean vaccinationPromptDue) {
}
