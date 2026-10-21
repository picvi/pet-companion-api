package com.picvi.pet_companion_service.medical.domain;

import java.time.LocalDate;
import java.util.UUID;

public record NextVetAppointment(UUID eventId, UUID visitId, LocalDate on, int startMinute, AppointmentStatus status) {
}
