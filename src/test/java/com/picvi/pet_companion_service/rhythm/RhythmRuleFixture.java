package com.picvi.pet_companion_service.rhythm;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.rhythm.domain.Range;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;

import java.util.List;

public final class RhythmRuleFixture {
    public static final RhythmRule ELEVEN_WEEKS = rule(10, 12, null, 4, 1, false, false);
    public static final RhythmRule ELEVEN_WEEKS_SMALL = rule(10, 12, SizeCategory.SMALL, 4, 1, true, false);
    public static final RhythmRule ELEVEN_WEEKS_LARGE = rule(10, 12, SizeCategory.LARGE, 4, 1, false, true);
    public static final RhythmRule TWENTY_WEEKS = rule(16, 26, null, 3, 0, false, false);

    private RhythmRuleFixture() {}

    public static RhythmRule rule(int fromWeeks,
                                  int toWeeks,
                                  SizeCategory sizeCategory,
                                  int standardMeals,
                                  int nightTrips,
                                  boolean mealSafetyNote,
                                  boolean softGroundPlay) {
        return new RhythmRule(fromWeeks, toWeeks, sizeCategory,
                new Range(45, 90, 75), new Range(60, 120, 90), new Range(2, 6, standardMeals),
                90, nightTrips, mealSafetyNote, softGroundPlay, List.of("RULE_OF_THUMB"));
    }
}
