package com.picvi.pet_companion_service.account.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(@NotBlank String password) {
    @Override
    public String toString() {
        return "DeleteAccountRequest[password=***]";
    }
}
