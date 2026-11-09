package com.picvi.pet_companion_service.medication.domain;

import java.time.LocalDate;

public record PlannedDose(LocalDate on, int minute) {
}
