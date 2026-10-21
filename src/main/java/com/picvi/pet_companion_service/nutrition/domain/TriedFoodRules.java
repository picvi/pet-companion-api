package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.exception.nutrition.FoodNotTriable;
import com.picvi.pet_companion_service.exception.nutrition.InvalidTriedFood;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TriedFoodRules {
    public static final int MAX_NOTE_LENGTH = 200;

    public void validate(SafetyLevel level, LocalDate triedOn, String note, LocalDate today) {
        if (level == SafetyLevel.NEVER) {
            throw new FoodNotTriable();
        }
        if (triedOn.isAfter(today)) {
            throw new InvalidTriedFood("The day she tried it cannot be in the future");
        }
        if (note != null && note.length() > MAX_NOTE_LENGTH) {
            throw new InvalidTriedFood("The note can be at most " + MAX_NOTE_LENGTH + " characters");
        }
    }
}
