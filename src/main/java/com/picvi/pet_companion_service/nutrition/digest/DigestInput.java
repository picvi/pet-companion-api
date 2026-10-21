package com.picvi.pet_companion_service.nutrition.digest;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;

import java.util.List;
import java.util.UUID;

/** One owner-added product, with only what may go into the admin's email: no owner or dog data except who counts as one owner. */
public record DigestInput(UUID productId,
                          UUID ownerId,
                          String brand,
                          String productName,
                          String flavour,
                          String normalisedName,
                          FoodForm form,
                          Completeness complete,
                          ProteinStatus proteinStatus,
                          List<IngredientCode> mainProteins) {
}
