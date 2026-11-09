package com.picvi.pet_companion_service.medical.domain;

import java.time.LocalDate;
import java.util.UUID;

public record PlannedAppointment(UUID visitId, UUID eventId, LocalDate on, int startMinute) {
}
