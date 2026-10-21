package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

final class HealthTestLogs {

    private HealthTestLogs() {
    }

    static LogSnapshot poo(LocalDate on, int minute, int stoolScore) {
        return poo(on, minute, stoolScore, Set.of());
    }

    static LogSnapshot poo(LocalDate on, int minute, int stoolScore, Set<LogFlag> flags) {
        return new LogSnapshot(UUID.randomUUID(), LogType.TOILET, LogDetail.POO, stoolScore, null, flags, on, minute, null, null, null, null);
    }

    static LogSnapshot meal(LocalDate on, int minute, LogDetail detail) {
        return new LogSnapshot(UUID.randomUUID(), LogType.MEAL, detail, null, null, Set.of(), on, minute, null, null, null, null);
    }

    static LogSnapshot symptom(LocalDate on, int minute, LogDetail detail) {
        return new LogSnapshot(UUID.randomUUID(), LogType.SYMPTOM, detail, null, null, Set.of(), on, minute, null, null, null, null);
    }

    static LogSnapshot of(LogType type, LocalDate on, int minute) {
        return new LogSnapshot(UUID.randomUUID(), type, null, null, null, Set.of(), on, minute, null, null, null, null);
    }
}
