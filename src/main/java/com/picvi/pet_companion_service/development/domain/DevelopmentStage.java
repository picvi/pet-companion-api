package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

public enum DevelopmentStage {
    HOMECOMING,
    FINDING_CONFIDENCE,
    TEETHING,
    ADOLESCENCE,
    YOUNG_ADULT;

    private static final Map<SizeCategory, Integer> ADOLESCENCE_END_WEEKS = Map.of(
            SizeCategory.SMALL, 52,
            SizeCategory.MEDIUM, 65,
            SizeCategory.LARGE, 78,
            SizeCategory.GIANT, 104);

    public static DevelopmentStage atAgeWeeks(int ageWeeks, SizeCategory size) {
        return Arrays.stream(values())
                .filter(stage -> stage.startWeek(size) <= ageWeeks)
                .reduce((earlier, later) -> later)
                .orElseThrow();
    }

    public int startWeek(SizeCategory size) {
        return switch (this) {
            case HOMECOMING -> 0;
            case FINDING_CONFIDENCE -> 10;
            case TEETHING -> 16;
            case ADOLESCENCE -> 26;
            case YOUNG_ADULT -> ADOLESCENCE_END_WEEKS.get(size);
        };
    }

    public Integer endWeek(SizeCategory size) {
        return next().map(nextStage -> nextStage.startWeek(size)).orElse(null);
    }

    public Optional<DevelopmentStage> next() {
        int nextOrdinal = ordinal() + 1;
        return nextOrdinal < values().length ? Optional.of(values()[nextOrdinal]) : Optional.empty();
    }
}
