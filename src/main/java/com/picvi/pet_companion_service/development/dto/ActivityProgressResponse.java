package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;

import java.time.LocalDate;

public record ActivityProgressResponse(String code, ProgressStatus status, Outcome lastOutcome,
                                       int attemptDays, LocalDate lastAttemptOn,
                                       LocalDate completedOn, LocalDate snoozedUntil) {

    public static ActivityProgressResponse of(String code, ProgressState state) {
        return new ActivityProgressResponse(code, state.status(), state.lastOutcome(), state.attemptDays(),
                state.lastAttemptOn(), state.completedOn(), state.snoozedUntil());
    }
}
