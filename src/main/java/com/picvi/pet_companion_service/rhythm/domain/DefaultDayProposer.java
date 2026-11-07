package com.picvi.pet_companion_service.rhythm.domain;

import java.util.ArrayList;
import java.util.List;

public class DefaultDayProposer {
    static final int WAKE_MINUTE = 7 * 60;
    static final int BED_MINUTE = 22 * 60 + 30;
    static final int LAST_MEAL_MINUTE = 18 * 60 + 30;

    public DaySettings propose(RhythmRule rule) {
        return new DaySettings(WAKE_MINUTE, BED_MINUTE, this.spreadMeals(rule.meals().standard()), null);
    }

    /** Her own wake and bed times, with the meals spread evenly over the day. */
    public DaySettings withMealCount(DaySettings current, int count) {
        int lastMeal = Math.min(LAST_MEAL_MINUTE, current.bedMinute() - DayPlanner.EVENING_WIND_DOWN_MINUTES - DayPlanner.MEAL_MINUTES);
        List<Integer> meals = this.spread(count, current.wakeMinute() + DayPlanner.MEAL_MINUTES, lastMeal);
        return new DaySettings(current.wakeMinute(), current.bedMinute(), meals, current.customAwakeMinutes());
    }

    private List<Integer> spreadMeals(int count) {
        return this.spread(count, WAKE_MINUTE + DayPlanner.MEAL_MINUTES, LAST_MEAL_MINUTE);
    }

    private List<Integer> spread(int count, int first, int last) {
        if (count == 1) {
            return List.of(first);
        }
        List<Integer> meals = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            meals.add(first + index * (last - first) / (count - 1));
        }
        return meals;
    }
}
