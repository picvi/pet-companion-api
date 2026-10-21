package com.picvi.pet_companion_service.medical.dto;

import java.time.LocalDate;
import java.util.List;

public record VetVisitListResponse(LocalDate date, List<VetVisitDto> visits, NextVetAppointmentDto nextVetAppointment) {
}
