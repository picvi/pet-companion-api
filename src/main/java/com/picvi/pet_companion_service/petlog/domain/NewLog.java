package com.picvi.pet_companion_service.petlog.domain;

import java.time.LocalDate;
import java.util.UUID;

public record NewLog(UUID clientId,
                     LogType type,
                     LogDetail detail,
                     LogValues values,
                     LocalDate localDate,
                     int minuteOfDay,
                     Integer utcOffsetMinutes,
                     CourseDose courseDose) {
}
