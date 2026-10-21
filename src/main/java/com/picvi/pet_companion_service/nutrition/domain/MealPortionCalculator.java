package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.api.MealPortion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** Splits the daily grams of the main food(s) over the day's meals. */
public class MealPortionCalculator {

    public record FoodShare(UUID petFoodId, Integer dailyGrams) {
    }

    public record Result(PortionState state, List<MealPortion> portions) {
    }

    /** Daily grams / meals, rounded half up; null when there is no daily amount. */
    public static Integer perMealGrams(Integer dailyGrams, int mealsPerDay) {
        if (dailyGrams == null || mealsPerDay < 1) {
            return null;
        }
        return BigDecimal.valueOf(dailyGrams).divide(BigDecimal.valueOf(mealsPerDay), 0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * USUAL when every main food has a daily amount. If any is missing, PARTS with no grams anywhere: half a
     * measured bowl and half "some" is not a plan the app can show.
     */
    public Result calculate(List<FoodShare> mainFoods, int mealsPerDay) {
        boolean allMeasured = mainFoods.stream().allMatch(food -> food.dailyGrams() != null);
        List<MealPortion> portions = mainFoods.stream()
                .map(food -> new MealPortion(food.petFoodId(), allMeasured ? perMealGrams(food.dailyGrams(), mealsPerDay) : null))
                .toList();
        return new Result(allMeasured ? PortionState.USUAL : PortionState.PARTS, portions);
    }
}
