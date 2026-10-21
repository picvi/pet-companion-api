package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.rule;
import static org.assertj.core.api.Assertions.assertThat;

class RhythmRuleResolverTest {
    private static final RhythmRule YOUNGEST = rule(8, 10, null, 4, 2, false, false);
    private static final RhythmRule MIDDLE = rule(10, 16, null, 4, 1, false, false);
    private static final RhythmRule MIDDLE_SMALL = rule(10, 16, SizeCategory.SMALL, 4, 1, true, false);
    private static final RhythmRule OLDEST = rule(16, 26, null, 3, 0, false, false);
    private static final List<RhythmRule> RULES = List.of(YOUNGEST, MIDDLE, MIDDLE_SMALL, OLDEST);

    private final RhythmRuleResolver resolver = new RhythmRuleResolver();

    @Test
    void picksTheBandThatCoversTheAge() {
        assertThat(this.resolver.resolve(RULES, 12, SizeCategory.MEDIUM)).contains(MIDDLE);
    }

    @Test
    void bandStartsAreInclusiveAndEndsExclusive() {
        assertThat(this.resolver.resolve(RULES, 10, SizeCategory.MEDIUM)).contains(MIDDLE);
        assertThat(this.resolver.resolve(RULES, 9, SizeCategory.MEDIUM)).contains(YOUNGEST);
        assertThat(this.resolver.resolve(RULES, 16, SizeCategory.MEDIUM)).contains(OLDEST);
    }

    @Test
    void prefersTheRuleForThePetsSizeOverTheGeneralOne() {
        assertThat(this.resolver.resolve(RULES, 12, SizeCategory.SMALL)).contains(MIDDLE_SMALL);
    }

    @Test
    void fallsBackToTheGeneralRuleWhenNoRuleExistsForTheSize() {
        assertThat(this.resolver.resolve(RULES, 20, SizeCategory.SMALL)).contains(OLDEST);
    }

    @Test
    void puppiesYoungerThanTheFirstBandUseTheFirstBand() {
        assertThat(this.resolver.resolve(RULES, 6, SizeCategory.MEDIUM)).contains(YOUNGEST);
    }

    @Test
    void nothingIsAvailableOnceTheLastBandEnds() {
        assertThat(this.resolver.resolve(RULES, 26, SizeCategory.MEDIUM)).isEmpty();
    }
}
