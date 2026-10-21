package com.picvi.pet_companion_service.development.domain;

public record Focus(FocusType type, ActivityDefinition activity, FocusReason reason, boolean doneToday) {
}
