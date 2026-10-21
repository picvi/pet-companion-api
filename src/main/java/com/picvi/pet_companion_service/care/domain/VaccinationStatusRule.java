package com.picvi.pet_companion_service.care.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

import java.util.Optional;

public class VaccinationStatusRule {

    public Optional<VaccinationStatus> statusFor(int doseCount, int seriesTotal) {
        if (doseCount <= 0) {
            return Optional.empty();
        }
        return Optional.of(doseCount >= seriesTotal ? VaccinationStatus.SERIES_COMPLETE : VaccinationStatus.FIRST_DOSE);
    }
}
