package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitProcedureKind;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.domain.VisitTestKind;
import com.picvi.pet_companion_service.medical.entity.VetVisit;
import com.picvi.pet_companion_service.medical.entity.VisitProcedure;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record VetVisitDto(UUID id,
                          UUID clientId,
                          VisitStatus status,
                          LocalDate visitedOn,
                          Integer scheduledMinute,
                          String clinic,
                          String note,
                          String prepNote,
                          boolean justCheck,
                          List<VisitOutcome> outcomes,
                          List<VisitLinks.CareEventRef> careEvents,
                          List<VisitLinks.ClinicMedicineDto> clinicMedicines,
                          Integer weightGrams,
                          List<VisitLinks.CourseRef> courses,
                          List<TestDto> tests,
                          ProcedureDto procedure,
                          UUID followUpOf,
                          UUID followUpPlanId,
                          AppointmentDto appointment,
                          long version) {

    public record TestDto(VisitTestKind kind, boolean waitingForResults) {
    }

    public record ProcedureDto(VisitProcedureKind kind, String note) {
        static ProcedureDto of(VisitProcedure procedure) {
            return new ProcedureDto(procedure.getKind(), procedure.getNote());
        }
    }

    /** Where a planned visit stands in her day, which the owner may have moved. */
    public record AppointmentDto(UUID eventId, LocalDate on, int startMinute) {
    }

    public static VetVisitDto of(VetVisit visit, DayEventApi.VetAppointment appointment, VisitLinks links, UUID followUpPlanId) {
        return new VetVisitDto(visit.getId(), visit.getClientId(), visit.getStatus(), visit.getVisitedOn(),
                visit.getScheduledMinute(), visit.getClinic(), visit.getNote(), visit.getPrepNote(), visit.isJustCheck(),
                visit.getOutcomes().stream().sorted(Comparator.naturalOrder()).toList(),
                links.careEvents(), links.clinicMedicines(), links.weightGrams(), links.courses(),
                visit.getTests().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .map(test -> new TestDto(test.getKey(), test.getValue()))
                        .toList(),
                visit.getProcedures().stream().findFirst().map(ProcedureDto::of).orElse(null),
                visit.getFollowUpOf(), followUpPlanId,
                appointment == null ? null : new AppointmentDto(appointment.eventId(), appointment.on(), appointment.startMinute()),
                visit.getVersion());
    }
}
