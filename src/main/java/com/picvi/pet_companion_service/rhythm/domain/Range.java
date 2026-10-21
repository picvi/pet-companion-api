package com.picvi.pet_companion_service.rhythm.domain;

public record Range(int min, int max, int standard) {

    public boolean contains(int value) {
        return value >= this.min && value <= this.max;
    }
}
