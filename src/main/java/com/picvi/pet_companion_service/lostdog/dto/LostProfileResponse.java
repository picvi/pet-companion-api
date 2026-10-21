package com.picvi.pet_companion_service.lostdog.dto;

import com.picvi.pet_companion_service.lostdog.domain.ChipRegistry;

import java.time.Instant;

public record LostProfileResponse(ChipRegistry chipRegistry, String chipRegistryName,
                                  String markings, Instant updatedAt) {}
