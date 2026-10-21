package com.picvi.pet_companion_service.petlog.domain;

import java.time.OffsetDateTime;

public record LoggedAt(OffsetDateTime occurredAt, int utcOffsetMinutes) {
}
