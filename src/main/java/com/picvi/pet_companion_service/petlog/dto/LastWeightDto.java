package com.picvi.pet_companion_service.petlog.dto;

import java.time.LocalDate;

public record LastWeightDto(Integer weightGrams, LocalDate localDate) {
}
