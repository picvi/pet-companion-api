package com.picvi.pet_companion_service.rhythm.dto;

import com.picvi.pet_companion_service.rhythm.domain.Range;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;

import java.util.List;

public record RhythmRuleDto(int fromWeeks,
                            int toWeeks,
                            Range awakeMinutes,
                            Range napMinutes,
                            Range meals,
                            int toiletIntervalMinutes,
                            int nightTrips,
                            List<String> sourceCodes) {

    public static RhythmRuleDto of(RhythmRule rule) {
        return new RhythmRuleDto(rule.fromWeeks(), rule.toWeeks(), rule.awakeMinutes(), rule.napMinutes(), rule.meals(),
                rule.toiletIntervalMinutes(), rule.nightTrips(), rule.sourceCodes());
    }
}
