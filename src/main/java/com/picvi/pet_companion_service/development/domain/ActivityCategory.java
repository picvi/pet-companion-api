package com.picvi.pet_companion_service.development.domain;

import java.util.Arrays;
import java.util.List;

public enum ActivityCategory {
    HANDLING(ActivityKind.SOCIALIZATION, ActivityGroup.CARE),
    SOUNDS(ActivityKind.SOCIALIZATION, ActivityGroup.AT_HOME),
    PEOPLE(ActivityKind.SOCIALIZATION, ActivityGroup.MEETING_OTHERS),
    SURFACES(ActivityKind.SOCIALIZATION, ActivityGroup.AT_HOME),
    PLACES(ActivityKind.SOCIALIZATION, ActivityGroup.OUT_AND_ABOUT),
    OBJECTS(ActivityKind.SOCIALIZATION, ActivityGroup.AT_HOME),
    TRAVEL(ActivityKind.SOCIALIZATION, ActivityGroup.OUT_AND_ABOUT),
    OTHER_ANIMALS(ActivityKind.SOCIALIZATION, ActivityGroup.MEETING_OTHERS),
    DOGS(ActivityKind.SOCIALIZATION, ActivityGroup.MEETING_OTHERS),
    HOME_LIFE(ActivityKind.SKILL, ActivityGroup.AT_HOME),
    FOUNDATION_CUES(ActivityKind.SKILL, ActivityGroup.TRAINING),
    LEAD_WALKING(ActivityKind.SKILL, ActivityGroup.TRAINING),
    MOUTH_MANNERS(ActivityKind.SKILL, ActivityGroup.TRAINING),
    IMPULSE_CONTROL(ActivityKind.SKILL, ActivityGroup.TRAINING),
    CARE(ActivityKind.SKILL, ActivityGroup.CARE);

    private final ActivityKind kind;
    private final ActivityGroup group;

    ActivityCategory(ActivityKind kind, ActivityGroup group) {
        this.kind = kind;
        this.group = group;
    }

    public ActivityKind kind() {
        return kind;
    }

    public ActivityGroup group() {
        return group;
    }

    public static List<ActivityCategory> rotationOrder(ActivityKind kind) {
        return Arrays.stream(values())
                .filter(category -> category.kind == kind)
                .toList();
    }
}
