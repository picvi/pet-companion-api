package com.picvi.pet_companion_service.medication.dto;

import java.time.LocalDate;

public record LastGivenDto(LocalDate on, int minute) {
}
