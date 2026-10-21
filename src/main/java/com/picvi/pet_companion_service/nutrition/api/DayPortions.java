package com.picvi.pet_companion_service.nutrition.api;

import com.picvi.pet_companion_service.nutrition.domain.PortionState;

import java.util.List;

public record DayPortions(PortionState state, List<MealPortion> portions) {

    public DayPortions withState(PortionState newState) {
        return new DayPortions(newState, this.portions);
    }
}
