package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityDisplayStatus;
import com.picvi.pet_companion_service.development.domain.ActivityGroup;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.ActivitySection;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.VaccinationRequirement;

import java.time.LocalDate;

public record DevelopmentActivityDto(String code, ActivityKind kind, ActivityCategory category,
                                     int startWeek, int endWeek, int difficulty,
                                     VaccinationRequirement vaccinationRequirement,
                                     ActivityDisplayStatus status, Outcome lastOutcome,
                                     LocalDate lastAttemptOn, LocalDate snoozedUntil,
                                     boolean recurring, boolean dueNow,
                                     ActivityGroup group, ActivitySection section, int rank,
                                     LocalDate availableFrom, boolean aloneTracker) {
}
