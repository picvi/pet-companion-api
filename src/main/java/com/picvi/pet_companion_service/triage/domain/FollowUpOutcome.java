package com.picvi.pet_companion_service.triage.domain;

public record FollowUpOutcome(Level level, CheckStatus status, FollowUpReason reason, Integer checkBackMinutes) {
}
