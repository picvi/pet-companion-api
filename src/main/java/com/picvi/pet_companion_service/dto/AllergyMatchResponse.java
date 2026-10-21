package com.picvi.pet_companion_service.dto;

import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;

import java.util.List;

public record AllergyMatchResponse(int status, String code, String message, List<AllergyMatch> matches) {
}
