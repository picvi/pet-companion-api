package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RhythmRuleResolver {

    public Optional<RhythmRule> resolve(List<RhythmRule> rules, int ageWeeks, SizeCategory sizeCategory) {
        int effectiveAge = Math.max(ageWeeks, this.youngestBandStart(rules));

        return rules.stream()
                .filter(rule -> rule.coversAge(effectiveAge))
                .filter(rule -> rule.sizeCategory() == null || rule.sizeCategory() == sizeCategory)
                .max(Comparator.comparing(rule -> rule.sizeCategory() != null));
    }

    private int youngestBandStart(List<RhythmRule> rules) {
        return rules.stream().mapToInt(RhythmRule::fromWeeks).min().orElse(0);
    }
}
