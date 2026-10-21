package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.exception.nutrition.InvalidPetFood;

import java.time.LocalDate;

/** What a pet's food entry must look like before it is saved. */
public class PetFoodRules {
    public static final int MAX_DAILY_GRAMS = 10000;

    public void validate(FoodKind kind, Integer dailyGrams, boolean forTraining, LocalDate startedOn, LocalDate today, LocalDate birthDate) {
        if (dailyGrams != null && (dailyGrams < 1 || dailyGrams > MAX_DAILY_GRAMS)) {
            throw new InvalidPetFood("dailyGrams must be between 1 and " + MAX_DAILY_GRAMS);
        }
        if (forTraining && kind != FoodKind.TREAT) {
            throw new InvalidPetFood("Only a treat can be marked as training food");
        }
        if (startedOn != null && (startedOn.isAfter(today) || startedOn.isBefore(birthDate))) {
            throw new InvalidPetFood("startedOn must be between the birth date and today");
        }
    }
}
