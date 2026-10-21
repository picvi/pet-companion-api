package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.nutrition.api.FoodChangeNow;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;

import java.time.LocalDate;
import java.util.List;

public record HealthSummary(CardState state, Headline headline, Note note, List<SignSummary> signs, FoodChangeNow foodChange) {

    public record Headline(BodySign sign, WatchKind kind, LogDetail detail, int count, LocalDate from, LocalDate lastOn) {
    }

    public record Note(NoteCode code, BodySign sign) {
    }

    public record SignSummary(BodySign sign, SignState state, SignWatch watch) {
    }

    public record SignWatch(WatchKind kind, LogDetail detail, int count, LocalDate from, LocalDate to, LocalDate lastOn,
                            int lastMinute, LocalDate normalSince) {
    }
}
