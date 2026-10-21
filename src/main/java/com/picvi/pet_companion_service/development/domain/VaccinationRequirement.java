package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;

public enum VaccinationRequirement {
    NONE,
    FIRST_DOSE,
    SERIES_COMPLETE;

    public boolean isMetBy(VaccinationStatus status) {
        return switch (this) {
            case NONE -> true;
            case FIRST_DOSE -> status == VaccinationStatus.FIRST_DOSE || status == VaccinationStatus.SERIES_COMPLETE;
            case SERIES_COMPLETE -> status == VaccinationStatus.SERIES_COMPLETE;
        };
    }
}
