package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.domain.AllergyMatcher.Ingredient;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.picvi.pet_companion_service.nutrition.domain.IngredientCode.*;
import static org.assertj.core.api.Assertions.assertThat;

class AllergyMatcherTest {
    private final AllergyMatcher matcher = new AllergyMatcher();

    private static Ingredient plain(IngredientCode code) {
        return new Ingredient(code, false);
    }

    @Test
    void theNamedAllergenIsContained() {
        assertThat(this.matcher.match(List.of(plain(SALMON), plain(CHICKEN)), List.of(CHICKEN)))
                .containsExactly(new AllergyMatch(CHICKEN, AllergyMatchKind.CONTAINS));
    }

    @Test
    void aTreatWithChickenAsAnOtherIngredientStillContainsIt() {
        assertThat(this.matcher.match(List.of(plain(BEEF), plain(CHICKEN), plain(RICE)), List.of(CHICKEN)))
                .containsExactly(new AllergyMatch(CHICKEN, AllergyMatchKind.CONTAINS));
    }

    @Test
    void aLabelThatNamesOnlyThePoultryMayContainChicken() {
        assertThat(this.matcher.match(List.of(plain(POULTRY)), List.of(CHICKEN)))
                .containsExactly(new AllergyMatch(CHICKEN, AllergyMatchKind.MAY_CONTAIN));
    }

    @Test
    void anAllergyToPoultryCoversEveryBird() {
        assertThat(this.matcher.match(List.of(plain(DUCK)), List.of(POULTRY)))
                .containsExactly(new AllergyMatch(POULTRY, AllergyMatchKind.CONTAINS));
    }

    @Test
    void hydrolysedChickenIsTheSofterNote() {
        assertThat(this.matcher.match(List.of(new Ingredient(CHICKEN, true)), List.of(CHICKEN)))
                .containsExactly(new AllergyMatch(CHICKEN, AllergyMatchKind.HYDROLYSED));
    }

    @Test
    void theStrongestMatchWinsPerAllergen() {
        assertThat(this.matcher.match(List.of(new Ingredient(CHICKEN, true), plain(POULTRY)), List.of(CHICKEN)))
                .containsExactly(new AllergyMatch(CHICKEN, AllergyMatchKind.MAY_CONTAIN));
    }

    @Test
    void unrelatedFoodsMatchNothing() {
        assertThat(this.matcher.match(List.of(plain(SALMON), plain(RICE)), List.of(CHICKEN, BEEF))).isEmpty();
        assertThat(this.matcher.match(List.of(plain(CHICKEN)), List.of())).isEmpty();
    }

    @Test
    void salmonDoesNotMatchAnAllergyToTrout() {
        assertThat(this.matcher.match(List.of(plain(SALMON)), List.of(TROUT))).isEmpty();
    }

    @Test
    void aFoodOnlySayingFishMayContainSalmon() {
        assertThat(this.matcher.match(List.of(plain(FISH)), List.of(SALMON)))
                .containsExactly(new AllergyMatch(SALMON, AllergyMatchKind.MAY_CONTAIN));
    }
}
