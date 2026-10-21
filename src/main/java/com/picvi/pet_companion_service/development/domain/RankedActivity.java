package com.picvi.pet_companion_service.development.domain;

import java.time.LocalDate;

public record RankedActivity(ActivityDefinition activity, ProgressState state, ActivitySection section,
                             int rank, LocalDate availableFrom) {
}
