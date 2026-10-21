package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Published after a log is saved, with a small snapshot so listeners never read the pet log tables. Deleting a log
 * publishes nothing: what depends on it is removed by the database (see food_transition_extension).
 */
public record PetLogSaved(UUID petId,
                          UUID logId,
                          LogType type,
                          LogDetail detail,
                          Integer stoolScore,
                          Wellbeing wellbeing,
                          Set<LogFlag> flags,
                          LocalDate localDate) {
}
