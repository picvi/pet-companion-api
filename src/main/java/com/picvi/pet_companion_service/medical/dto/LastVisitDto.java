package com.picvi.pet_companion_service.medical.dto;

import com.picvi.pet_companion_service.medical.entity.VetVisit;

import java.time.LocalDate;
import java.util.UUID;

public record LastVisitDto(UUID id, LocalDate visitedOn, String clinic) {

    public static LastVisitDto of(VetVisit visit) {
        return new LastVisitDto(visit.getId(), visit.getVisitedOn(), visit.getClinic());
    }
}
