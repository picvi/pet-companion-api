package com.picvi.pet_companion_service.medical.domain;

import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;

import java.time.LocalDate;

public class VetVisitRules {
    public static final int MAX_CLINIC_LENGTH = 120;
    public static final int MAX_NOTE_LENGTH = 500;
    public static final int MAX_PREP_NOTE_LENGTH = 300;
    public static final int MAX_PLAN_YEARS = 2;
    public static final int MAX_PLAN_MINUTE = 1440 - 60;

    public void validatePlan(LocalDate scheduledOn, Integer scheduledMinute, String clinic, String prepNote, LocalDate clientDate) {
        if (scheduledOn.isBefore(clientDate) || scheduledOn.isAfter(clientDate.plusYears(MAX_PLAN_YEARS))) {
            throw new InvalidVetVisit("scheduledOn", InvalidVetVisit.OUT_OF_RANGE);
        }
        if (scheduledMinute < 0 || scheduledMinute > MAX_PLAN_MINUTE) {
            throw new InvalidVetVisit("scheduledMinute", InvalidVetVisit.OUT_OF_RANGE);
        }
        if (clinic != null && clinic.length() > MAX_CLINIC_LENGTH) {
            throw new InvalidVetVisit("clinic", InvalidVetVisit.TOO_LONG);
        }
        if (prepNote != null && prepNote.length() > MAX_PREP_NOTE_LENGTH) {
            throw new InvalidVetVisit("prepNote", InvalidVetVisit.TOO_LONG);
        }
    }
}
