package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.ReviewStatus;

import java.time.LocalDate;

public record Review(ReviewStatus status, String reviewedBy, LocalDate reviewedOn) {
}
