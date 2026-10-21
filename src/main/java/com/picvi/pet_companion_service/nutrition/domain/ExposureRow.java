package com.picvi.pet_companion_service.nutrition.domain;

import java.time.LocalDate;

/** One ingredient of one food a pet was given, as read from v_pet_ingredient_exposure. toOn is the first day it was no longer given. */
public record ExposureRow(IngredientCode code, FoodKind foodKind, boolean hydrolysed, LocalDate fromOn, LocalDate toOn) {
}
