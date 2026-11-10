package com.picvi.pet_companion_service.medical.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** What a visit points at in other modules, already read in batches. */
public record VisitLinks(List<CareEventRef> careEvents,
                         List<ClinicMedicineDto> clinicMedicines,
                         Integer weightGrams,
                         List<CourseRef> courses) {

    public record CareEventRef(UUID id, String itemCode, LocalDate performedOn) {
    }

    public record ClinicMedicineDto(UUID logId, String label, String dose) {
    }

    public record CourseRef(UUID id, String name, String status) {
    }

    public static VisitLinks none() {
        return new VisitLinks(List.of(), List.of(), null, List.of());
    }
}
