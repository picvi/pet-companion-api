package com.picvi.pet_companion_service.timeline.dto;

import com.picvi.pet_companion_service.care.dto.CareTimelineRow;
import com.picvi.pet_companion_service.development.dto.ActivityTimelineRow;
import com.picvi.pet_companion_service.medical.api.MedicalApi.VisitSnapshot;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.timeline.domain.TimelineItem;

import com.picvi.pet_companion_service.nutrition.api.FoodChangeEvent;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import com.picvi.pet_companion_service.timeline.domain.LostTimelineRow;
import com.picvi.pet_companion_service.triage.api.TriageApi.CheckSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TimelineItemDto(String kind,
                              UUID id,
                              LocalDate localDate,
                              Integer minuteOfDay,
                              PetLogDto log,
                              ActivityTimelineRow activity,
                              CareTimelineRow care,
                              FoodChangeEvent food,
                              VisitDto visit,
                              LostDto lost,
                              CheckSnapshot check) {

    public record VisitDto(List<VisitOutcome> outcomes, String clinic) {
        static VisitDto of(VisitSnapshot visit) {
            return new VisitDto(visit.outcomes(), visit.clinic());
        }
    }

    public record LostDto(LostStatus status, java.time.Instant startedAt, java.time.Instant endedAt,
                          long durationSeconds, String lastSeenPlace) {
        static LostDto of(LostTimelineRow lost) {
            return new LostDto(lost.status(), lost.startedAt().toInstant(java.time.ZoneOffset.UTC), lost.endedAt(),
                    lost.durationSeconds(), lost.lastSeenPlace());
        }
    }

    public static TimelineItemDto of(TimelineItem item) {
        Integer minuteOfDay = item.minuteOfDay() == TimelineItem.NO_TIME ? null : item.minuteOfDay();
        return new TimelineItemDto(item.kind().name(), item.id(), item.localDate(), minuteOfDay, item.log(), item.activity(),
                item.care(), item.food(),
                item.visit() == null ? null : VisitDto.of(item.visit()),
                item.lost() == null ? null : LostDto.of(item.lost()), item.check());
    }
}
