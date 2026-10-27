package com.picvi.pet_companion_service.nutrition.digest;

import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FoodDigestFormatterTest {
    private final FoodDigestFormatter formatter = new FoodDigestFormatter();

    private static DigestInput input(UUID owner, String name, ProteinStatus protein, IngredientCode... mains) {
        return new DigestInput(UUID.randomUUID(), owner, null, name, null, name.toLowerCase(), FoodForm.DRY, Completeness.UNKNOWN,
                protein, List.of(mains));
    }

    @Test
    void threeOwnersAddingTheSameFoodAreOneLineWithACount() {
        List<DigestEntry> entries = this.formatter.entries(List.of(
                input(UUID.randomUUID(), "Karma od hodowcy", ProteinStatus.KNOWN, IngredientCode.CHICKEN),
                input(UUID.randomUUID(), "Karma od hodowcy", ProteinStatus.KNOWN, IngredientCode.CHICKEN),
                input(UUID.randomUUID(), "Karma od hodowcy", ProteinStatus.KNOWN, IngredientCode.BEEF)));

        assertThat(entries).singleElement().satisfies(entry -> {
            assertThat(entry.owners()).isEqualTo(3);
            assertThat(entry.productIds()).hasSize(3);
            assertThat(entry.proteins()).containsExactly(IngredientCode.CHICKEN, IngredientCode.BEEF);
        });
        assertThat(this.formatter.body(entries)).contains("x3  Karma od hodowcy");
    }

    @Test
    void oneOwnerAddingItTwiceCountsAsOneOwner() {
        UUID owner = UUID.randomUUID();

        List<DigestEntry> entries = this.formatter.entries(List.of(
                input(owner, "Karma", ProteinStatus.KNOWN, IngredientCode.CHICKEN), input(owner, "Karma", ProteinStatus.KNOWN, IngredientCode.CHICKEN)));

        assertThat(entries.get(0).owners()).isEqualTo(1);
        assertThat(entries.get(0).productIds()).hasSize(2);
    }

    @Test
    void notSureIsSaidInPlainWords() {
        List<DigestEntry> entries = this.formatter.entries(List.of(input(UUID.randomUUID(), "Mystery", ProteinStatus.UNKNOWN)));

        assertThat(this.formatter.body(entries)).contains("proteins: not sure");
    }

    @Test
    void theEmailNamesNoOwner() {
        UUID owner = UUID.randomUUID();
        List<DigestEntry> entries = this.formatter.entries(List.of(input(owner, "Karma", ProteinStatus.KNOWN, IngredientCode.CHICKEN)));

        assertThat(this.formatter.body(entries)).doesNotContain(owner.toString());
    }

    @Test
    void theSubjectCountsTheFoods() {
        List<DigestEntry> two = this.formatter.entries(List.of(
                input(UUID.randomUUID(), "A", ProteinStatus.KNOWN, IngredientCode.CHICKEN), input(UUID.randomUUID(), "B", ProteinStatus.KNOWN, IngredientCode.CHICKEN)));

        assertThat(this.formatter.subject(two)).isEqualTo("curl up: 2 new owner-added foods");
        assertThat(this.formatter.subject(two.subList(0, 1))).isEqualTo("curl up: 1 new owner-added food");
    }
}
