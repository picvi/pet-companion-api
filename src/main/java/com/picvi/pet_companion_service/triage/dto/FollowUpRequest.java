package com.picvi.pet_companion_service.triage.dto;

import com.picvi.pet_companion_service.triage.domain.FollowUpAnswer;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FollowUpRequest(@NotNull UUID clientId, @NotNull FollowUpAnswer answer) {
}
