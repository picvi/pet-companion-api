package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.exception.nutrition.InvalidFoodProduct;
import com.picvi.pet_companion_service.exception.nutrition.ProteinRequired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** What an owner-added product must look like before it is saved. */
public class FoodProductRules {

    /** @return KNOWN when a main protein is named, UNKNOWN when the owner said "not sure". */
    public ProteinStatus validate(List<IngredientInput> ingredients, boolean proteinNotSure) {
        Set<IngredientCode> seen = new HashSet<>();
        boolean hasMain = false;
        for (IngredientInput ingredient : ingredients) {
            if (!seen.add(ingredient.code())) {
                throw new InvalidFoodProduct(ingredient.code() + " is listed twice");
            }
            if (ingredient.role() == IngredientRole.MAIN) {
                if (ingredient.code().getKind() != IngredientKind.PROTEIN) {
                    throw new InvalidFoodProduct(ingredient.code() + " is not a protein, so it cannot be the main protein");
                }
                hasMain = true;
            }
        }
        if (proteinNotSure && hasMain) {
            throw new InvalidFoodProduct("A main protein and \"not sure\" cannot be sent together");
        }
        if (!proteinNotSure && !hasMain) {
            throw new ProteinRequired();
        }
        return proteinNotSure ? ProteinStatus.UNKNOWN : ProteinStatus.KNOWN;
    }

    public record IngredientInput(IngredientCode code, IngredientRole role) {
    }
}
