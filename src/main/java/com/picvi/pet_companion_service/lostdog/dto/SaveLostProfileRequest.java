package com.picvi.pet_companion_service.lostdog.dto;

import com.picvi.pet_companion_service.lostdog.domain.ChipRegistry;
import jakarta.validation.constraints.Size;

public record SaveLostProfileRequest(
        ChipRegistry chipRegistry,
        @Size(max = 100) String chipRegistryName,
        @Size(max = 300) String markings) {}
