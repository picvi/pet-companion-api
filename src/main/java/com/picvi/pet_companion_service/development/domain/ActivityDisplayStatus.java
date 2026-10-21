package com.picvi.pet_companion_service.development.domain;

public enum ActivityDisplayStatus {
    TODO,
    IN_PROGRESS,
    COMPLETED,
    NOT_POSSIBLE,
    LOCKED;

    public static ActivityDisplayStatus of(ProgressStatus status, boolean locked) {
        return switch (status) {
            case COMPLETED -> COMPLETED;
            case NOT_POSSIBLE -> NOT_POSSIBLE;
            case IN_PROGRESS -> locked ? LOCKED : IN_PROGRESS;
            case NOT_STARTED -> locked ? LOCKED : TODO;
        };
    }
}
