package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;

public class DiaryFilter {

    public boolean shows(PetLogDto log) {
        return switch (log.type()) {
            case TOILET -> log.detail() == LogDetail.POO || log.detail() == LogDetail.BOTH || log.stoolScore() != null;
            case MEAL -> log.detail() == LogDetail.LEFT_SOME || log.detail() == LogDetail.REFUSED;
            case SYMPTOM, MEDICINE -> true;
            case SLEEP, WAKE, WALK, WEIGHT, NOTE -> false;
        };
    }
}
