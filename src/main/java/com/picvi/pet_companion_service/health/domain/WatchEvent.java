package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;

import java.time.LocalDate;

public record WatchEvent(WatchKind kind, LogDetail detail, BodySign sign, LocalDate on, int minute) {
}
