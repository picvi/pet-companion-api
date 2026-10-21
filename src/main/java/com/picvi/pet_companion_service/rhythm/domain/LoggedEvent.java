package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.petlog.domain.LogType;

public record LoggedEvent(LogType type, int minute) {
}
