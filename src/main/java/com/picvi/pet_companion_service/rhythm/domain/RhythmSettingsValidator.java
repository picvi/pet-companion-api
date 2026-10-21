package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.exception.rhythm.AwakeOutOfRange;
import com.picvi.pet_companion_service.exception.rhythm.InvalidDayTimes;
import com.picvi.pet_companion_service.exception.rhythm.MealOutsideDay;
import com.picvi.pet_companion_service.exception.rhythm.MealsOutOfRange;

import java.util.List;

public class RhythmSettingsValidator {
    static final int MIN_DAY_MINUTES = 10 * 60;
    static final int MAX_DAY_MINUTES = 20 * 60;

    public void validate(RhythmRule rule, DaySettings settings) {
        int wakeMinute = settings.wakeMinute();
        int bedMinute = settings.bedMinute();
        List<Integer> mealMinutes = settings.mealMinutes();
        Integer customAwakeMinutes = settings.customAwakeMinutes();
        int dayLength = bedMinute - wakeMinute;
        if (dayLength < MIN_DAY_MINUTES || dayLength > MAX_DAY_MINUTES || bedMinute >= DayPlanner.MINUTES_PER_DAY) {
            throw new InvalidDayTimes();
        }
        if (mealMinutes.stream().distinct().count() != mealMinutes.size() || !rule.meals().contains(mealMinutes.size())) {
            throw new MealsOutOfRange();
        }
        int lastMealMinute = bedMinute - DayPlanner.EVENING_WIND_DOWN_MINUTES - DayPlanner.MEAL_MINUTES;
        if (mealMinutes.stream().anyMatch(meal -> meal < wakeMinute || meal > lastMealMinute)) {
            throw new MealOutsideDay();
        }
        if (customAwakeMinutes != null && !rule.awakeMinutes().contains(customAwakeMinutes)) {
            throw new AwakeOutOfRange();
        }
    }
}
