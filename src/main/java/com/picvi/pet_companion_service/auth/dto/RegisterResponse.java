package com.picvi.pet_companion_service.auth.dto;

public record RegisterResponse(String email, boolean verificationRequired) {
}
