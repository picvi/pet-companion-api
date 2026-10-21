package com.picvi.pet_companion_service.rhythm.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;

import java.util.List;

public record RhythmRule(int fromWeeks,
                         int toWeeks,
                         SizeCategory sizeCategory,
                         Range awakeMinutes,
                         Range napMinutes,
                         Range meals,
                         int toiletIntervalMinutes,
                         int nightTrips,
                         boolean mealSafetyNote,
                         boolean softGroundPlay,
                         List<String> sourceCodes) {

    public boolean coversAge(int ageWeeks) {
        return ageWeeks >= this.fromWeeks && ageWeeks < this.toWeeks;
    }
}
