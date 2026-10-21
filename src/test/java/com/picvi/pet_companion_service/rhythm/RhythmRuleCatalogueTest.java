package com.picvi.pet_companion_service.rhythm;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.rhythm.domain.Range;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RhythmRuleCatalogueTest {
    private static final Set<String> KNOWN_SOURCES = Set.of(
            "TUFTS_FEEDING", "PETMD_FEEDING", "FIRSTVET_HYPOGLYCAEMIA", "VCA_LARGE_BREED",
            "PENNVET_HOUSETRAINING", "KRONTVEIT_2012", "AAHA_SLEEP_2026", "RULE_OF_THUMB");

    private final List<RhythmRule> rules = RhythmCatalogueFixture.load();

    @Test
    void everyRowOfTheSeedIsRead() {
        assertThat(this.rules).hasSize(16);
    }

    @Test
    void everyWeekFromEightToTwentyFiveHasARuleForEverySize() {
        RhythmRuleResolver resolver = new RhythmRuleResolver();

        for (int week = 8; week < 26; week++) {
            for (SizeCategory size : SizeCategory.values()) {
                assertThat(resolver.resolve(this.rules, week, size)).as("week %d, %s", week, size).isPresent();
            }
        }
    }

    @Test
    void theGeneralBandsAreContiguous() {
        List<RhythmRule> general = this.rules.stream().filter(rule -> rule.sizeCategory() == null)
                .sorted((first, second) -> Integer.compare(first.fromWeeks(), second.fromWeeks())).toList();

        for (int index = 1; index < general.size(); index++) {
            assertThat(general.get(index).fromWeeks()).isEqualTo(general.get(index - 1).toWeeks());
        }
    }

    @Test
    void everySizeRuleHasAGeneralRuleForTheSameBand() {
        assertThat(this.rules).filteredOn(rule -> rule.sizeCategory() != null).allSatisfy(rule ->
                assertThat(this.rules).anyMatch(general -> general.sizeCategory() == null
                        && general.fromWeeks() == rule.fromWeeks() && general.toWeeks() == rule.toWeeks()));
    }

    @Test
    void standardValuesStayInsideTheirRanges() {
        assertThat(this.rules).allSatisfy(rule -> {
            assertThat(inside(rule.awakeMinutes())).isTrue();
            assertThat(inside(rule.napMinutes())).isTrue();
            assertThat(inside(rule.meals())).isTrue();
        });
    }

    @Test
    void everyRuleNamesItsSourcesFromTheKnownList() {
        assertThat(this.rules).allSatisfy(rule -> {
            assertThat(rule.sourceCodes()).isNotEmpty();
            assertThat(KNOWN_SOURCES).containsAll(rule.sourceCodes());
        });
    }

    @Test
    void smallPuppiesGetAtLeastFourMealsUntilSixteenWeeksAndASafetyNoteUnderTwelve() {
        assertThat(this.rules).filteredOn(rule -> rule.sizeCategory() == SizeCategory.SMALL && rule.fromWeeks() < 16)
                .allSatisfy(rule -> assertThat(rule.meals().min()).isGreaterThanOrEqualTo(4));
        assertThat(this.rules).filteredOn(rule -> rule.sizeCategory() == SizeCategory.SMALL)
                .allSatisfy(rule -> assertThat(rule.mealSafetyNote()).isEqualTo(rule.fromWeeks() < 12));
    }

    @Test
    void onlyLargeAndGiantPuppiesGetTheSoftGroundNote() {
        assertThat(this.rules).allSatisfy(rule -> assertThat(rule.softGroundPlay())
                .isEqualTo(rule.sizeCategory() == SizeCategory.LARGE || rule.sizeCategory() == SizeCategory.GIANT));
    }

    private static boolean inside(Range range) {
        return range.min() <= range.standard() && range.standard() <= range.max();
    }
}
