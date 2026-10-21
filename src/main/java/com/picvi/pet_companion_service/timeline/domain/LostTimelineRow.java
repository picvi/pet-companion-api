package com.picvi.pet_companion_service.timeline.domain;

import com.picvi.pet_companion_service.lostdog.domain.LostStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record LostTimelineRow(UUID id, LocalDateTime startedAt, Instant endedAt, LostStatus status,
                              long durationSeconds, String lastSeenPlace) {}
