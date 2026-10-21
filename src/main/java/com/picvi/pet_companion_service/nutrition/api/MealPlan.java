package com.picvi.pet_companion_service.nutrition.api;

/** What goes in the bowl today and tomorrow. dayOfPlan and totalDays are set while a food change is running. */
public record MealPlan(DayPortions today, DayPortions tomorrow, Integer dayOfPlan, Integer totalDays) {
}
