package com.picvi.pet_companion_service.nutrition.domain;

/** The pet's allergen that a food touches, and how. */
public record AllergyMatch(IngredientCode code, AllergyMatchKind match) {
}
