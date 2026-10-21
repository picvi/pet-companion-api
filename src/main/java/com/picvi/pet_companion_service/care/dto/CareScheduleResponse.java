package com.picvi.pet_companion_service.care.dto;

import java.time.LocalDate;
import java.util.List;

public record CareScheduleResponse(LocalDate date, int ageWeeks, String alertCode, List<CareItemDto> items) {
}
