package com.picvi.pet_companion_service.auth.dto;

public record LoginResponse(String accessToken, String refreshToken, UserResponse user) {
}
