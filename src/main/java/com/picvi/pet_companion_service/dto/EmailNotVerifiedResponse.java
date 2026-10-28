package com.picvi.pet_companion_service.dto;

/** An {@link ErrorResponse} that also carries the address to send the confirmation code to. */
public record EmailNotVerifiedResponse(
        int status,
        String code,
        String message,
        String email
) {
}
