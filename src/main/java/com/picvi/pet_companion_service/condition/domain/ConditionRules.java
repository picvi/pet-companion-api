package com.picvi.pet_companion_service.condition.domain;

import com.picvi.pet_companion_service.exception.condition.InvalidCondition;

import java.time.LocalDate;

public class ConditionRules {
    public static final int TEXT_MAX = 200;

    /** A condition names an ingredient (allergy, intolerance only) or describes itself in words. */
    public void validate(ConditionKind kind, String ingredientCode, String text, LocalDate since, LocalDate resolvedOn, LocalDate today) {
        if (ingredientCode == null && text == null) {
            throw new InvalidCondition("Name an ingredient or describe the condition");
        }
        if (ingredientCode != null && kind == ConditionKind.CHRONIC) {
            throw new InvalidCondition("A chronic condition is described in words, not by an ingredient");
        }
        if (text != null && text.length() > TEXT_MAX) {
            throw new InvalidCondition("text is longer than " + TEXT_MAX + " characters");
        }
        if (since != null && since.isAfter(today)) {
            throw new InvalidCondition("since cannot be in the future");
        }
        if (since != null && resolvedOn != null && resolvedOn.isBefore(since)) {
            throw new InvalidCondition("resolvedOn cannot be before since");
        }
    }
}
