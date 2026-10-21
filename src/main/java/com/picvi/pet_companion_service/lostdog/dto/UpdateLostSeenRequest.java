package com.picvi.pet_companion_service.lostdog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record UpdateLostSeenRequest(
        @NotNull Instant lastSeenAt,
        @NotBlank @Size(max = 300) String lastSeenPlace,
        Double lastSeenLat,
        Double lastSeenLng,
        @Pattern(regexp = "^$|[+0-9 ()-]{7,32}") String posterPhone,
        @Size(max = 300) String posterMessage) {}
