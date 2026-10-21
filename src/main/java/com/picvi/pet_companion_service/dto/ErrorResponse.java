package com.picvi.pet_companion_service.dto;

public record ErrorResponse(
        int status,
        String code,
        String message
) {
}
