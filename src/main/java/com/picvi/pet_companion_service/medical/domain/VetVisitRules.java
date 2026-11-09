package com.picvi.pet_companion_service.medical.domain;

import com.picvi.pet_companion_service.exception.medical.InvalidVetVisit;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest;

import java.time.LocalDate;

public class VetVisitRules {
    public static final int MAX_CLINIC_LENGTH = 120;
    public static final int MAX_NOTE_LENGTH = 500;
    public static final int MAX_PREP_NOTE_LENGTH = 300;
    public static final int MAX_PLAN_YEARS = 2;
    public static final int MAX_PLAN_MINUTE = 1440 - 60;
    public static final int MAX_CLINIC_MEDICINES = 5;
    public static final int MAX_HOME_COURSES = 3;

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

    public void validateLog(LogVisitRequest request) {
        if (request.visitedOn().isAfter(request.date())) {
            throw new InvalidVetVisit("visitedOn", InvalidVetVisit.OUT_OF_RANGE);
        }
        if (request.visitMinute() != null && (request.visitMinute() < 0 || request.visitMinute() > 1439)) {
            throw new InvalidVetVisit("visitMinute", InvalidVetVisit.OUT_OF_RANGE);
        }
        this.validateLength("clinic", request.clinic(), MAX_CLINIC_LENGTH);
        this.validateLength("note", request.note(), MAX_NOTE_LENGTH);
        this.validateItems(request);
        this.validateJustCheck(request);
        this.validateFollowUp(request);
    }

    private void validateItems(LogVisitRequest request) {
        if (request.clinicMedicines().size() > MAX_CLINIC_MEDICINES) {
            throw new InvalidVetVisit("clinicMedicines", InvalidVetVisit.TOO_MANY);
        }
        if (request.homeCourses().size() > MAX_HOME_COURSES) {
            throw new InvalidVetVisit("homeCourses", InvalidVetVisit.TOO_MANY);
        }
        if (request.careItemCodes().stream().anyMatch(String::isBlank)) {
            throw new InvalidVetVisit("careItemCodes", InvalidVetVisit.INVALID);
        }
        if (request.clinicMedicines().stream().anyMatch(medicine -> medicine.label() == null || medicine.label().isBlank())) {
            throw new InvalidVetVisit("clinicMedicines", InvalidVetVisit.REQUIRED);
        }
        if (request.tests().stream().map(LogVisitRequest.TestRequest::kind).distinct().count() != request.tests().size()) {
            throw new InvalidVetVisit("tests", InvalidVetVisit.INVALID);
        }
        LogVisitRequest.Procedure procedure = request.procedure();
        if (procedure != null && procedure.kind() == VisitProcedureKind.OTHER && (procedure.note() == null || procedure.note().isBlank())) {
            throw new InvalidVetVisit("procedure.note", InvalidVetVisit.REQUIRED);
        }
        if (procedure != null) {
            this.validateLength("procedure.note", procedure.note(), MAX_NOTE_LENGTH);
        }
    }

    private void validateJustCheck(LogVisitRequest request) {
        boolean hasLists = !request.careItemCodes().isEmpty() || !request.clinicMedicines().isEmpty()
                || !request.homeCourses().isEmpty() || !request.tests().isEmpty();
        if (request.justCheck() && (hasLists || request.procedure() != null)) {
            throw new InvalidVetVisit("justCheck", InvalidVetVisit.INVALID);
        }
    }

    private void validateFollowUp(LogVisitRequest request) {
        LogVisitRequest.FollowUp followUp = request.followUp();
        if (followUp == null) {
            return;
        }
        if (followUp.on().isBefore(request.visitedOn()) || followUp.on().isAfter(request.date().plusYears(MAX_PLAN_YEARS))) {
            throw new InvalidVetVisit("followUp.on", InvalidVetVisit.OUT_OF_RANGE);
        }
        if (followUp.minute() < 0 || followUp.minute() > MAX_PLAN_MINUTE) {
            throw new InvalidVetVisit("followUp.minute", InvalidVetVisit.OUT_OF_RANGE);
        }
    }

    private void validateLength(String field, String text, int max) {
        if (text != null && text.length() > max) {
            throw new InvalidVetVisit(field, InvalidVetVisit.TOO_LONG);
        }
    }
}
