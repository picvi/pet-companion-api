package com.picvi.pet_companion_service.medical.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/** {@code date} is the client's today. {@code clientId} makes a retried plan safe and is not sent when a plan is changed. */
public record PlanVisitRequest(UUID clientId,
                               @NotNull LocalDate date,
                               @NotNull LocalDate scheduledOn,
                               @NotNull Integer scheduledMinute,
                               String clinic,
                               String prepNote) {
}
