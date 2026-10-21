package com.picvi.pet_companion_service.lostdog.dto;

import jakarta.validation.constraints.NotNull;

public record SetLostStepRequest(@NotNull Boolean done) {}
