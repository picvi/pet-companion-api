package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.AllergyMatch;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.FoodOrigin;
import com.picvi.pet_companion_service.nutrition.domain.MealPortionCalculator;
import com.picvi.pet_companion_service.nutrition.entity.PetFood;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PetFoodDto(UUID id,
                         FoodKind kind,
                         FoodOrigin origin,
                         LocalDate startedOn,
                         LocalDate endedOn,
                         Integer dailyGrams,
                         Integer perMealGrams,
                         boolean forTraining,
                         long version,
                         FoodProductDto product,
                         List<AllergyMatch> allergyMatches) {

    public static PetFoodDto of(PetFood food, FoodProductDto product, int mealsPerDay) {
        Integer perMeal = food.getKind() == FoodKind.MAIN ? MealPortionCalculator.perMealGrams(food.getDailyGrams(), mealsPerDay) : null;
        return new PetFoodDto(food.getId(), food.getKind(), food.getOrigin(), food.getStartedOn(), food.getEndedOn(),
                food.getDailyGrams(), perMeal, food.isForTraining(), food.getVersion(), product, product.allergyMatches());
    }
}
