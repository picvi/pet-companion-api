package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/** How other modules write to the pet log. Logging again with the same client id returns the log that was saved first. */
@Component
@RequiredArgsConstructor
public class PetLogRecorder {
    private final PetLogService logService;

    public UUID logMedicine(UUID ownerId, UUID petId, UUID clientId, String label, String dose,
                            LocalDate localDate, int minuteOfDay, LocalDate today) {
        LogValues values = new LogValues(null, null, null, null, null, null, null, label, dose, null);
        return this.log(ownerId, petId, clientId, LogType.MEDICINE, values, localDate, minuteOfDay, today);
    }

    public UUID logWeight(UUID ownerId, UUID petId, UUID clientId, int weightGrams,
                          LocalDate localDate, int minuteOfDay, LocalDate today) {
        LogValues values = new LogValues(null, null, null, null, weightGrams, null, null, null, null, null);
        return this.log(ownerId, petId, clientId, LogType.WEIGHT, values, localDate, minuteOfDay, today);
    }

    public void delete(UUID ownerId, UUID petId, UUID logId, LocalDate today) {
        this.logService.undo(ownerId, petId, logId, today);
    }

    private UUID log(UUID ownerId, UUID petId, UUID clientId, LogType type, LogValues values,
                     LocalDate localDate, int minuteOfDay, LocalDate today) {
        NewLog newLog = new NewLog(clientId, type, null, values, localDate, minuteOfDay, null, null);
        return this.logService.log(ownerId, petId, newLog, today).log().id();
    }
}
