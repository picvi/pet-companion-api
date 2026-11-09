package com.picvi.pet_companion_service.care.api;

import com.picvi.pet_companion_service.care.dto.CareEventDto;
import com.picvi.pet_companion_service.care.dto.CareScheduleResponse;
import com.picvi.pet_companion_service.care.service.CareEventService;
import com.picvi.pet_companion_service.care.service.CareScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** What other modules may ask about a pet's vaccines and parasite treatments. */
@Component
@RequiredArgsConstructor
public class CareApi {
    private final CareScheduleService scheduleService;
    private final CareEventService eventService;

    public CareScheduleResponse schedule(UUID ownerId, UUID petId, LocalDate date) {
        return this.scheduleService.getSchedule(ownerId, petId, date);
    }

    public CoreVaccineProgress coreVaccineDoses(UUID ownerId, UUID petId, LocalDate date) {
        return this.scheduleService.coreVaccineDoses(ownerId, petId, date);
    }

    public CareEventDto log(UUID ownerId, UUID petId, String itemCode, LocalDate performedOn, LocalDate date) {
        return this.eventService.log(ownerId, petId, itemCode, performedOn, date);
    }

    public void undo(UUID ownerId, UUID petId, UUID eventId, LocalDate date) {
        this.eventService.undo(ownerId, petId, eventId, date);
    }

    public Optional<CareEventDto> findEvent(UUID petId, UUID eventId) {
        return this.eventService.find(petId, eventId);
    }

    public List<CareEventDto> events(UUID petId, Collection<UUID> eventIds) {
        return this.eventService.findAll(petId, eventIds);
    }
}
