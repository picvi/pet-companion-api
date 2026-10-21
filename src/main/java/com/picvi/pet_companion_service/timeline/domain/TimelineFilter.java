package com.picvi.pet_companion_service.timeline.domain;

import com.picvi.pet_companion_service.petlog.domain.LogType;

import java.util.EnumSet;
import java.util.Set;

public enum TimelineFilter {
    ALL(EnumSet.allOf(LogType.class), false, true, true, true, true, true, true),
    HEALTH(EnumSet.of(LogType.SYMPTOM, LogType.MEDICINE, LogType.WEIGHT), true, false, true, true, false, true, true),
    FOOD(EnumSet.of(LogType.MEAL), false, false, false, true, false, false, false),
    TOILET(EnumSet.of(LogType.TOILET), false, false, false, false, false, false, false),
    GROWING(EnumSet.of(LogType.WEIGHT), false, true, false, false, false, false, false);

    private final Set<LogType> logTypes;
    private final boolean flaggedToilet;
    private final boolean activities;
    private final boolean care;
    private final boolean foodChanges;
    private final boolean visits;
    private final boolean lostReports;
    private final boolean checks;

    TimelineFilter(Set<LogType> logTypes, boolean flaggedToilet, boolean activities, boolean care, boolean foodChanges,
                   boolean visits, boolean lostReports, boolean checks) {
        this.logTypes = logTypes;
        this.flaggedToilet = flaggedToilet;
        this.activities = activities;
        this.care = care;
        this.foodChanges = foodChanges;
        this.visits = visits;
        this.lostReports = lostReports;
        this.checks = checks;
    }

    public Set<LogType> logTypes() {
        return this.logTypes;
    }

    public boolean includesFlaggedToilet() {
        return this.flaggedToilet;
    }

    public boolean includesActivities() {
        return this.activities;
    }

    public boolean includesCare() {
        return this.care;
    }

    public boolean includesFoodChanges() {
        return this.foodChanges;
    }

    public boolean includesVisits() {
        return this.visits;
    }

    public boolean includesLostReports() {
        return this.lostReports;
    }

    public boolean includesChecks() { return this.checks; }
}
