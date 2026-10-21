package com.picvi.pet_companion_service.pet.entity;

public enum VaccinationStatus {
    UNKNOWN,
    NONE,
    FIRST_DOSE,
    SERIES_COMPLETE;

    public boolean isLowerThan(VaccinationStatus other) {
        return this.compareTo(other) < 0;
    }
}
