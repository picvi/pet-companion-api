package com.picvi.pet_companion_service.medical.dto;

import java.time.LocalDate;
import java.util.List;

/** {@code nextBefore} is the {@code before} of the next page, or {@code null} on the last one. */
public record VisitHistoryPage(List<VetVisitDto> visits, LocalDate nextBefore) {
}
