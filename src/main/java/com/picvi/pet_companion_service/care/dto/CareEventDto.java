package com.picvi.pet_companion_service.care.dto;

import com.picvi.pet_companion_service.care.entity.CareEvent;

import java.time.LocalDate;
import java.util.UUID;

public record CareEventDto(UUID id, String itemCode, LocalDate performedOn) {

    public static CareEventDto of(CareEvent event) {
        return new CareEventDto(event.getId(), event.getItem().getCode(), event.getPerformedOn());
    }
}
