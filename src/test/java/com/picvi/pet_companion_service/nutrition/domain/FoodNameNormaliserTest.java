package com.picvi.pet_companion_service.nutrition.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoodNameNormaliserTest {

    @Test
    void polishDiacriticsAndCaseAreIgnored() {
        assertThat(FoodNameNormaliser.normalise("KACZKĄ")).isEqualTo("kaczka");
        assertThat(FoodNameNormaliser.normalise("kaczka")).isEqualTo("kaczka");
        assertThat(FoodNameNormaliser.normalise("Dolina Noteci", "Łosoś")).isEqualTo("dolina noteci losos");
    }

    @Test
    void punctuationBecomesASpaceAndSpacesCollapse() {
        assertThat(FoodNameNormaliser.normalise("Brit Care - Puppy  (Salmon & Rice)")).isEqualTo("brit care puppy salmon rice");
        assertThat(FoodNameNormaliser.normalise("Grain-Free")).isEqualTo("grain free");
    }

    @Test
    void nullPartsAreSkipped() {
        assertThat(FoodNameNormaliser.normalise(null, "Karma od hodowcy", null)).isEqualTo("karma od hodowcy");
        assertThat(FoodNameNormaliser.normalise((String) null)).isEmpty();
    }
}
