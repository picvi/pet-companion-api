package com.picvi.pet_companion_service.development.domain;

public record PlanItem(ActivityDefinition activity, PlanReason reason, boolean doneToday) {
}
