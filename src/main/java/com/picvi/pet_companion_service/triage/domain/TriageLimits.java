package com.picvi.pet_companion_service.triage.domain;

public final class TriageLimits {
    public static final int RELATED_HOURS = 24;
    public static final int RELATED_DAYS = 7;
    public static final int MAX_SAME_BEFORE_CALL = 2;
    public static final int MIN_REPEAT_MINUTES = 60;
    public static final int LAPSE_HOURS = 24;
    public static final int BACKFILL_DAYS = 7;
    public static final int NOTE_MAX = 500;

    private TriageLimits() {
    }
}
