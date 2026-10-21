package com.picvi.pet_companion_service.auth.dto;

import java.util.UUID;

public record UserResponse(UUID id, String username, String email, boolean hasPet) {
}
