package com.picvi.pet_companion_service.nutrition.domain;

public enum ChangePhase {
    /** The plan has not started yet; the old food is still the only one. */
    BEFORE_START,
    RUNNING,
    /** The last day has passed; the new food is the only one. */
    OVER
}
