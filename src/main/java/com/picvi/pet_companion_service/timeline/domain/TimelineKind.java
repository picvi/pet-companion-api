package com.picvi.pet_companion_service.timeline.domain;

import lombok.Getter;

@Getter
public enum TimelineKind {
    LOST(-3),
    VISIT(-2),
    FOOD(-1),
    CARE(0),
    ACTIVITY(1),
    LOG(2),
    CHECK(3);

    private final int order;

    TimelineKind(int order) {
        this.order = order;
    }
}
