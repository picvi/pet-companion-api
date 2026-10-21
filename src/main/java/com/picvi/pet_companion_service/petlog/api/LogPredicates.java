package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;

/** Shared concern predicates for other modules. */
public final class LogPredicates {
    public static final int LOOSE_STOOL_SCORE = 5;

    private LogPredicates() {
    }

    public static boolean looseStool(PetLogApi.LogSnapshot log) {
        return log.type() == LogType.TOILET && log.stoolScore() != null && log.stoolScore() >= LOOSE_STOOL_SCORE;
    }

    public static boolean vomiting(PetLogApi.LogSnapshot log) {
        return log.type() == LogType.SYMPTOM && log.detail() == LogDetail.VOMITING;
    }

    public static boolean leftFood(PetLogApi.LogSnapshot log) {
        return log.type() == LogType.MEAL && (log.detail() == LogDetail.LEFT_SOME || log.detail() == LogDetail.REFUSED);
    }

    public static boolean lowEnergy(PetLogApi.LogSnapshot log) {
        return log.type() == LogType.SYMPTOM && log.detail() == LogDetail.LOW_ENERGY;
    }
}
