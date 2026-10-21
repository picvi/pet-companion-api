package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.medical.domain.BriefHelpful;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitReason;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** {@code date} is the client's today; {@code clientId} makes a retried create safe. */
public record CreateVetVisitRequest(@NotNull UUID clientId,
                                    @NotNull LocalDate date,
                                    @NotNull LocalDate visitedOn,
                                    @NotNull VisitReason reason,
                                    List<VisitOutcome> outcomes,
                                    String clinic,
                                    String note,
                                    LocalDate nextCheckOn,
                                    Integer nextCheckMinute,
                                    BriefHelpful briefHelpful) {

    public CreateVetVisitRequest {
        outcomes = outcomes == null ? List.of() : outcomes;
    }
}
