package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.PlanItem;
import com.picvi.pet_companion_service.development.domain.PlanReason;

public record PlanItemDto(ActivityRefDto activity, PlanReason reason, boolean doneToday) {

    public static PlanItemDto of(PlanItem item) {
        return new PlanItemDto(ActivityRefDto.of(item.activity()), item.reason(), item.doneToday());
    }
}
