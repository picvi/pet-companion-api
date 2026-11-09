package com.picvi.pet_companion_service.medical.dto;

import java.time.LocalDate;
import java.util.List;

/** What Home, Health and her day need: the planned visits, the last visit and the next appointment, never the history. */
public record UpcomingVisitsResponse(LocalDate date,
                                     List<VetVisitDto> plans,
                                     LastVisitDto lastVisit,
                                     NextVetAppointmentDto nextVetAppointment) {
}
