package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.medical.domain.AppointmentStatus;
import com.picvi.pet_companion_service.medical.domain.NextVetAppointment;

import java.time.LocalDate;
import java.util.UUID;

public record NextVetAppointmentDto(UUID eventId, UUID visitId, LocalDate on, int startMinute, AppointmentStatus status) {

    public static NextVetAppointmentDto of(NextVetAppointment appointment) {
        return new NextVetAppointmentDto(appointment.eventId(), appointment.visitId(), appointment.on(), appointment.startMinute(),
                appointment.status());
    }
}
