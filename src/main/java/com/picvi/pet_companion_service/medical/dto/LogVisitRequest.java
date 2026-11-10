package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.medical.domain.VisitProcedureKind;
import com.picvi.pet_companion_service.medical.domain.VisitTestKind;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * {@code date} is the client's today. {@code clientId} makes a retried log safe and is not sent when a visit is changed.
 * When a visit is changed, the lists and {@code weightGrams} hold only what is new.
 */
public record LogVisitRequest(UUID clientId,
                              @NotNull LocalDate date,
                              @NotNull LocalDate visitedOn,
                              Integer visitMinute,
                              String clinic,
                              String note,
                              boolean justCheck,
                              List<String> careItemCodes,
                              List<ClinicMedicine> clinicMedicines,
                              List<HomeCourse> homeCourses,
                              Integer weightGrams,
                              List<TestRequest> tests,
                              Procedure procedure,
                              FollowUp followUp) {

    public LogVisitRequest {
        clinic = blankToNull(clinic);
        note = blankToNull(note);
        careItemCodes = careItemCodes == null ? List.of() : List.copyOf(careItemCodes);
        clinicMedicines = clinicMedicines == null ? List.of() : List.copyOf(clinicMedicines);
        homeCourses = homeCourses == null ? List.of() : List.copyOf(homeCourses);
        tests = tests == null ? List.of() : List.copyOf(tests);
    }

    public record ClinicMedicine(String label, String dose) {
    }

    public record TestRequest(@NotNull VisitTestKind kind, boolean waitingForResults) {
    }

    public record Procedure(@NotNull VisitProcedureKind kind, String note) {
    }

    public record FollowUp(@NotNull LocalDate on, @NotNull Integer minute) {
    }

    public record HomeCourse(@NotNull UUID clientId,
                             String name,
                             Integer amountHundredths,
                             DoseUnit unit,
                             DoseSchedule schedule,
                             Integer everyDays,
                             List<Integer> times,
                             WithFood withFood,
                             LocalDate startsOn,
                             Integer startMinute,
                             LocalDate endsOn,
                             Integer endsMinute,
                             Boolean reminders) {

        public CreateCourseRequest toCreateRequest(LocalDate date) {
            return new CreateCourseRequest(this.clientId, date, this.name, this.amountHundredths, this.unit, this.schedule,
                    this.everyDays, this.times, this.withFood, this.startsOn, this.startMinute, this.endsOn, this.endsMinute,
                    this.reminders);
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
