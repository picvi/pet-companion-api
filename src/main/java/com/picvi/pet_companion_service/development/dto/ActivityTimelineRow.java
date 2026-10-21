package com.picvi.pet_companion_service.development.dto;

import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.entity.PetActivityLog;

import java.time.LocalDate;
import java.util.UUID;

public record ActivityTimelineRow(UUID id, LocalDate performedOn, String activityCode, Outcome outcome, Integer durationSeconds) {

    public static ActivityTimelineRow of(PetActivityLog log) {
        return new ActivityTimelineRow(log.getId(), log.getPerformedOn(), log.getActivity().getCode(), log.getOutcome(),
                log.getDurationSeconds());
    }
}
