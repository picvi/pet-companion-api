package com.picvi.pet_companion_service.medical.domain;

import java.time.LocalDate;
import java.util.UUID;

public record BookedAppointment(UUID eventId, LocalDate on, int startMinute, UUID bookedByVisitId) {
}
