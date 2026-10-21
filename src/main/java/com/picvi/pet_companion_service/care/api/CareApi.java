package com.picvi.pet_companion_service.care.api;

import com.picvi.pet_companion_service.care.dto.CareScheduleResponse;
import com.picvi.pet_companion_service.care.service.CareScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/** What other modules may ask about a pet's vaccines and parasite treatments. */
@Component
@RequiredArgsConstructor
public class CareApi {
    private final CareScheduleService scheduleService;

    public CareScheduleResponse schedule(UUID ownerId, UUID petId, LocalDate date) {
        return this.scheduleService.getSchedule(ownerId, petId, date);
    }

    public CoreVaccineProgress coreVaccineDoses(UUID ownerId, UUID petId, LocalDate date) {
        return this.scheduleService.coreVaccineDoses(ownerId, petId, date);
    }
}
