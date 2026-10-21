package com.picvi.pet_companion_service.petlog.domain;

import java.time.LocalDate;

public record LogEdit(LogDetail detail,
                      LogValues values,
                      LocalDate localDate,
                      int minuteOfDay,
                      Integer utcOffsetMinutes) {
}
