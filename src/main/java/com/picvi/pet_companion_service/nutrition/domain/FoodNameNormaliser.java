package com.picvi.pet_companion_service.nutrition.domain;

import java.text.Normalizer;
import java.util.Locale;

/** Same result as the f_food_normalise SQL function: lower case, no accents, punctuation to spaces, one space between words. */
public final class FoodNameNormaliser {
    private FoodNameNormaliser() {}

    public static String normalise(String... parts) {
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                joined.append(part).append(' ');
            }
        }
        String lower = joined.toString().toLowerCase(Locale.ROOT)
                .replace('ł', 'l').replace('ø', 'o').replace('đ', 'd');
        String unaccented = Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return unaccented.replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
    }
}
