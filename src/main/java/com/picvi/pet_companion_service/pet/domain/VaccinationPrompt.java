package com.picvi.pet_companion_service.pet.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

public class VaccinationPrompt {
    private static final int[] THRESHOLDS = {16, 18, 20};

    public boolean isDue(VaccinationStatus status, int promptCount, int ageWeeks) {
        return status != VaccinationStatus.SERIES_COMPLETE
                && promptCount < THRESHOLDS.length
                && ageWeeks >= THRESHOLDS[promptCount];
    }
}
