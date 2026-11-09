package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.medical.domain.BriefHelpful;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/** A full replace of the editable fields. {@code date} is the client's today. */
public record UpdateVetVisitRequest(@NotNull LocalDate date,
                                    @NotNull LocalDate visitedOn,
                                    List<VisitOutcome> outcomes,
                                    String clinic,
                                    String note,
                                    LocalDate nextCheckOn,
                                    Integer nextCheckMinute,
                                    BriefHelpful briefHelpful) {

    public UpdateVetVisitRequest {
        outcomes = outcomes == null ? List.of() : outcomes;
    }
}
