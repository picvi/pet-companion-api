package com.picvi.pet_companion_service.nutrition.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Product ingredients + the pet's allergens → matches. A food that names the allergen, or a kind of it (allergy
 * POULTRY, food with CHICKEN), CONTAINS it. A food that names only the broader group (allergy CHICKEN, food with
 * POULTRY) MAY_CONTAIN it. A hydrolysed ingredient is reported as HYDROLYSED, a softer note rather than silence.
 * Each allergen is reported once, with the strongest match.
 */
public class AllergyMatcher {

    public record Ingredient(IngredientCode code, boolean hydrolysed) {
    }

    public List<AllergyMatch> match(List<Ingredient> ingredients, List<IngredientCode> allergens) {
        List<AllergyMatch> matches = new ArrayList<>();
        for (IngredientCode allergen : allergens) {
            ingredients.stream()
                    .map(ingredient -> this.kindOf(ingredient, allergen))
                    .flatMap(Optional::stream)
                    .min(java.util.Comparator.naturalOrder())
                    .ifPresent(kind -> matches.add(new AllergyMatch(allergen, kind)));
        }
        return matches;
    }

    private Optional<AllergyMatchKind> kindOf(Ingredient ingredient, IngredientCode allergen) {
        boolean contains = isSameOrChildOf(ingredient.code(), allergen);
        boolean mayContain = !contains && isSameOrChildOf(allergen, ingredient.code());
        if (!contains && !mayContain) {
            return Optional.empty();
        }
        if (ingredient.hydrolysed()) {
            return Optional.of(AllergyMatchKind.HYDROLYSED);
        }
        return Optional.of(contains ? AllergyMatchKind.CONTAINS : AllergyMatchKind.MAY_CONTAIN);
    }

    private static boolean isSameOrChildOf(IngredientCode code, IngredientCode ancestor) {
        for (IngredientCode current = code; current != null; current = current.parent()) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }
}
