package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.time.LocalDate;
import java.util.List;

public record DevelopmentResponse(LocalDate date, int ageWeeks, StageDto stage, NextStageDto nextStage,
                                  StageProgressDto progress, VaccinationStatus vaccinationStatus,
                                  List<DevelopmentActivityDto> activities, List<StageWindowDto> stages) {
}
