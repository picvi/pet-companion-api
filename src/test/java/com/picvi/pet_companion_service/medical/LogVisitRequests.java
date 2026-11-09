package com.picvi.pet_companion_service.medical;

import com.picvi.pet_companion_service.medical.dto.LogVisitRequest;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.ClinicMedicine;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.FollowUp;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.HomeCourse;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.Procedure;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.TestRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class LogVisitRequests {
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private UUID clientId = UUID.randomUUID();
    private LocalDate visitedOn = TODAY;
    private Integer visitMinute = 600;
    private String clinic = "VetCare";
    private String note;
    private boolean justCheck;
    private List<String> careItemCodes = List.of();
    private List<ClinicMedicine> clinicMedicines = List.of();
    private List<HomeCourse> homeCourses = List.of();
    private Integer weightGrams;
    private List<TestRequest> tests = List.of();
    private Procedure procedure;
    private FollowUp followUp;

    private LogVisitRequests() {
    }

    public static LogVisitRequests aVisit() {
        return new LogVisitRequests();
    }

    public LogVisitRequests clientId(UUID clientId) {
        this.clientId = clientId;
        return this;
    }

    public LogVisitRequests visitedOn(LocalDate visitedOn) {
        this.visitedOn = visitedOn;
        return this;
    }

    public LogVisitRequests visitMinute(Integer visitMinute) {
        this.visitMinute = visitMinute;
        return this;
    }

    public LogVisitRequests clinic(String clinic) {
        this.clinic = clinic;
        return this;
    }

    public LogVisitRequests note(String note) {
        this.note = note;
        return this;
    }

    public LogVisitRequests justCheck() {
        this.justCheck = true;
        return this;
    }

    public LogVisitRequests careItemCodes(String... codes) {
        this.careItemCodes = List.of(codes);
        return this;
    }

    public LogVisitRequests clinicMedicines(ClinicMedicine... medicines) {
        this.clinicMedicines = List.of(medicines);
        return this;
    }

    public LogVisitRequests homeCourses(HomeCourse... courses) {
        this.homeCourses = List.of(courses);
        return this;
    }

    public LogVisitRequests weightGrams(Integer weightGrams) {
        this.weightGrams = weightGrams;
        return this;
    }

    public LogVisitRequests tests(TestRequest... tests) {
        this.tests = List.of(tests);
        return this;
    }

    public LogVisitRequests procedure(Procedure procedure) {
        this.procedure = procedure;
        return this;
    }

    public LogVisitRequests followUp(LocalDate on, Integer minute) {
        this.followUp = new FollowUp(on, minute);
        return this;
    }

    public LogVisitRequest build() {
        return new LogVisitRequest(this.clientId, TODAY, this.visitedOn, this.visitMinute, this.clinic, this.note, this.justCheck,
                this.careItemCodes, this.clinicMedicines, this.homeCourses, this.weightGrams, this.tests, this.procedure, this.followUp);
    }

    public static HomeCourse aCourse(String name) {
        return new HomeCourse(UUID.randomUUID(), name, 50, null, null, null, List.of(480), null, TODAY, 480, TODAY.plusDays(7), 480, true);
    }
}
