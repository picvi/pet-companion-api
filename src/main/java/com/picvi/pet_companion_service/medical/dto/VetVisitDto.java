package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.dayevent.api.DayEventApi;
import com.picvi.pet_companion_service.medical.domain.BriefHelpful;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.entity.VetVisit;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record VetVisitDto(UUID id,
                          UUID clientId,
                          LocalDate visitedOn,
                          List<VisitOutcome> outcomes,
                          String clinic,
                          String note,
                          LocalDate nextCheckOn,
                          Integer nextCheckMinute,
                          BriefHelpful briefHelpful,
                          long version) {

    public static VetVisitDto of(VetVisit visit, DayEventApi.VetAppointment appointment) {
        return new VetVisitDto(visit.getId(), visit.getClientId(), visit.getVisitedOn(),
                visit.getOutcomes().stream().sorted(Comparator.naturalOrder()).toList(), visit.getClinic(), visit.getNote(),
                appointment == null ? null : appointment.on(), appointment == null ? null : appointment.startMinute(),
                visit.getBriefHelpful(), visit.getVersion());
    }
}
