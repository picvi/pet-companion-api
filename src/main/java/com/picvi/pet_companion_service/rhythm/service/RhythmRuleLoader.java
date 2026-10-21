package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.rhythm.domain.Range;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRule;
import com.picvi.pet_companion_service.rhythm.entity.RhythmRuleRow;
import com.picvi.pet_companion_service.rhythm.repository.RhythmRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RhythmRuleLoader {
    private final RhythmRuleRepository ruleRepository;

    public List<RhythmRule> loadAll() {
        return this.ruleRepository.findAll().stream().map(this::toRule).toList();
    }

    private RhythmRule toRule(RhythmRuleRow row) {
        return new RhythmRule(
                row.getFromWeeks(),
                row.getToWeeks(),
                row.getSizeCategory(),
                new Range(row.getAwakeMin(), row.getAwakeMax(), row.getAwakeStandard()),
                new Range(row.getNapMin(), row.getNapMax(), row.getNapStandard()),
                new Range(row.getMealsMin(), row.getMealsMax(), row.getMealsStandard()),
                row.getToiletIntervalMinutes(),
                row.getNightTrips(),
                row.isMealSafetyNote(),
                row.isSoftGroundPlay(),
                Arrays.asList(row.getSourceCodes().split(","))
        );
    }
}
