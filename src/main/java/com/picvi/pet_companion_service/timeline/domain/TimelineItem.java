package com.picvi.pet_companion_service.timeline.domain;

import com.picvi.pet_companion_service.care.dto.CareTimelineRow;
import com.picvi.pet_companion_service.development.dto.ActivityTimelineRow;
import com.picvi.pet_companion_service.medical.api.MedicalApi.VisitSnapshot;
import com.picvi.pet_companion_service.lostdog.domain.LostStatus;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;

import com.picvi.pet_companion_service.nutrition.api.FoodChangeEvent;
import com.picvi.pet_companion_service.triage.api.TriageApi.CheckSnapshot;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.UUID;

public record TimelineItem(TimelineKind kind,
                           UUID id,
                           LocalDate localDate,
                           int minuteOfDay,
                           PetLogDto log,
                           ActivityTimelineRow activity,
                           CareTimelineRow care,
                           FoodChangeEvent food,
                           VisitSnapshot visit,
                           LostTimelineRow lost,
                           CheckSnapshot check) {
    public static final int NO_TIME = -1;
    public static final int VISIT_MINUTE = 0;
    public static final Comparator<TimelineItem> NEWEST_FIRST = Comparator
            .comparing(TimelineItem::localDate, Comparator.reverseOrder())
            .thenComparing(TimelineItem::minuteOfDay, Comparator.reverseOrder())
            .thenComparing(item -> item.kind().getOrder(), Comparator.reverseOrder());

    public static TimelineItem of(PetLogDto log) {
        return new TimelineItem(TimelineKind.LOG, log.id(), log.localDate(), log.minuteOfDay(), log, null, null, null, null, null, null);
    }

    public static TimelineItem of(ActivityTimelineRow activity) {
        return new TimelineItem(TimelineKind.ACTIVITY, activity.id(), activity.performedOn(), NO_TIME, null, activity, null, null, null, null, null);
    }

    public static TimelineItem of(CareTimelineRow care) {
        return new TimelineItem(TimelineKind.CARE, care.id(), care.performedOn(), NO_TIME, null, null, care, null, null, null, null);
    }

    public static TimelineItem of(FoodChangeEvent food) {
        return new TimelineItem(TimelineKind.FOOD, food.id(), food.date(), NO_TIME, null, null, null, food, null, null, null);
    }

    public static TimelineItem of(VisitSnapshot visit) {
        return new TimelineItem(TimelineKind.VISIT, visit.id(), visit.visitedOn(), VISIT_MINUTE, null, null, null, null, visit, null, null);
    }

    public static TimelineItem of(LostTimelineRow lost) {
        return new TimelineItem(TimelineKind.LOST, lost.id(), lost.startedAt().toLocalDate(), lost.startedAt().getHour() * 60 + lost.startedAt().getMinute(),
                null, null, null, null, null, lost, null);
    }

    public static TimelineItem of(CheckSnapshot check) {
        return new TimelineItem(TimelineKind.CHECK, check.id(), check.occurredOn(), check.minuteOfDay(),
                null, null, null, null, null, null, check);
    }

    public TimelineCursor cursor() {
        return new TimelineCursor(this.localDate, this.minuteOfDay, this.kind, this.id);
    }
}

