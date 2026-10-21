package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.TriedReaction;
import com.picvi.pet_companion_service.nutrition.entity.PetTriedFood;

import java.time.LocalDate;

public record TriedFoodDto(String foodCode, LocalDate triedOn, TriedReaction reaction, String note) {

    public static TriedFoodDto of(PetTriedFood food) {
        return new TriedFoodDto(food.getFoodCode(), food.getTriedOn(), food.getReaction(), food.getNote());
    }
}
