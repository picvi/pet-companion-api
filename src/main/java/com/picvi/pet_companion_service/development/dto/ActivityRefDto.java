package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.domain.ActivityGroup;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.AloneLadder;

public record ActivityRefDto(String code, ActivityKind kind, ActivityCategory category, int difficulty,
                             ActivityGroup group, boolean aloneTracker) {

    public static ActivityRefDto of(ActivityDefinition activity) {
        return activity == null ? null
                : new ActivityRefDto(activity.code(), activity.kind(), activity.category(), activity.difficulty(),
                activity.category().group(), AloneLadder.isAloneActivity(activity.code()));
    }
}
