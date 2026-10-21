package com.picvi.pet_companion_service.nutrition.digest;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;

import java.util.List;
import java.util.UUID;

/** One line of the digest: a food as owners typed it, and how many owners typed it. */
public record DigestEntry(String name,
                          FoodForm form,
                          Completeness complete,
                          List<IngredientCode> proteins,
                          boolean proteinNotSure,
                          int owners,
                          List<UUID> productIds) {
}
