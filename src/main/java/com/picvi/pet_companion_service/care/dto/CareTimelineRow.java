package com.picvi.pet_companion_service.care.dto;

import com.picvi.pet_companion_service.care.entity.CareEvent;

import java.time.LocalDate;
import java.util.UUID;

public record CareTimelineRow(UUID id, LocalDate performedOn, String itemCode) {

    public static CareTimelineRow of(CareEvent event) {
        return new CareTimelineRow(event.getId(), event.getPerformedOn(), event.getItem().getCode());
    }
}
