package com.picvi.pet_companion_service.medical.domain;

import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VetVisitRules {
    public static final int MAX_CLINIC_LENGTH = 120;
    public static final int MAX_NOTE_LENGTH = 500;
    public static final int MAX_NEXT_CHECK_YEARS = 2;
    public static final int MAX_NEXT_CHECK_MINUTE = 1440 - 60;

    public void validate(LocalDate visitedOn, LocalDate nextCheckOn, Integer nextCheckMinute, List<VisitOutcome> outcomes,
                         String clinic, String note, LocalDate birthDate, LocalDate clientDate) {
        if (visitedOn.isAfter(clientDate.plusDays(1)) || (birthDate != null && visitedOn.isBefore(birthDate))) {
            throw new InvalidVetVisit("visitedOn", InvalidVetVisit.OUT_OF_RANGE);
        }
        if (nextCheckOn == null && nextCheckMinute != null) {
            throw new InvalidVetVisit("nextCheckOn", InvalidVetVisit.REQUIRED);
        }
        if (nextCheckOn != null && nextCheckMinute == null) {
            throw new InvalidVetVisit("nextCheckMinute", InvalidVetVisit.REQUIRED);
        }
        if (nextCheckOn != null && (!nextCheckOn.isAfter(visitedOn) || nextCheckOn.isBefore(clientDate)
                || nextCheckOn.isAfter(clientDate.plusYears(MAX_NEXT_CHECK_YEARS)))) {
            throw new InvalidVetVisit("nextCheckOn", InvalidVetVisit.OUT_OF_RANGE);
        }
        if (nextCheckMinute != null && (nextCheckMinute < 0 || nextCheckMinute > MAX_NEXT_CHECK_MINUTE)) {
            throw new InvalidVetVisit("nextCheckMinute", InvalidVetVisit.OUT_OF_RANGE);
        }
        this.validateOutcomes(outcomes);
        if (clinic != null && clinic.length() > MAX_CLINIC_LENGTH) {
            throw new InvalidVetVisit("clinic", InvalidVetVisit.TOO_LONG);
        }
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new InvalidVetVisit("note", InvalidVetVisit.TOO_LONG);
        }
    }

    private void validateOutcomes(List<VisitOutcome> outcomes) {
        Set<VisitOutcome> distinct = new HashSet<>();
        for (VisitOutcome outcome : outcomes) {
            if (outcome == null || !distinct.add(outcome)) {
                throw new InvalidVetVisit("outcomes", InvalidVetVisit.INVALID);
            }
        }
        if (distinct.contains(VisitOutcome.FINE) && distinct.size() > 1) {
            throw new InvalidVetVisit("outcomes", InvalidVetVisit.INVALID);
        }
    }
}
