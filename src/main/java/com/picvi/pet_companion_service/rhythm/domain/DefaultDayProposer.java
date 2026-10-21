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

    private List<Integer> spreadMeals(int count) {
        int first = WAKE_MINUTE + DayPlanner.MEAL_MINUTES;
        if (count == 1) {
            return List.of(first);
        }
        List<Integer> meals = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            meals.add(first + index * (LAST_MEAL_MINUTE - first) / (count - 1));
        }
        return meals;
    }
}
