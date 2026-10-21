package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class OccurredAtCalculator {
    private static final int MIN_OFFSET_MINUTES = -720;
    private static final int MAX_OFFSET_MINUTES = 840;

    private final ZoneId fallbackZone;

    public OccurredAtCalculator(ZoneId fallbackZone) {
        this.fallbackZone = fallbackZone;
    }

    public LoggedAt at(LocalDate localDate, int minuteOfDay, Integer utcOffsetMinutes) {
        if (utcOffsetMinutes != null && (utcOffsetMinutes < MIN_OFFSET_MINUTES || utcOffsetMinutes > MAX_OFFSET_MINUTES)) {
            throw new InvalidLog(LogField.UTC_OFFSET_MINUTES, InvalidLog.OUT_OF_RANGE);
        }
        LocalDateTime local = localDate.atStartOfDay().plusMinutes(minuteOfDay);
        ZoneOffset offset = utcOffsetMinutes == null
                ? this.fallbackZone.getRules().getOffset(local)
                : ZoneOffset.ofTotalSeconds(utcOffsetMinutes * 60);
        return new LoggedAt(OffsetDateTime.of(local, offset), offset.getTotalSeconds() / 60);
    }
}
