package com.picvi.pet_companion_service.timeline.service;

import com.picvi.pet_companion_service.care.service.CareEventService;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.service.ActivityProgressService;
import com.picvi.pet_companion_service.medical.api.MedicalApi;
import com.picvi.pet_companion_service.lostdog.service.LostDogService;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.domain.LogCursor;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import com.picvi.pet_companion_service.timeline.domain.TimelineCursor;
import com.picvi.pet_companion_service.timeline.domain.TimelineFilter;
import com.picvi.pet_companion_service.timeline.domain.TimelineItem;
import com.picvi.pet_companion_service.timeline.domain.TimelineKind;
import com.picvi.pet_companion_service.timeline.dto.TimelineItemDto;
import com.picvi.pet_companion_service.timeline.dto.TimelineResponse;
import com.picvi.pet_companion_service.triage.api.TriageApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TimelineService {
    private static final UUID ZERO_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final int PAGE_SIZE = 50;

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final PetLogService logService;
    private final ActivityProgressService activityService;
    private final CareEventService careService;
    private final NutritionApi nutritionApi;
    private final MedicalApi medicalApi;
    private final TriageApi triageApi;
    private LostDogService lostDogService;

    @Autowired
    void setLostDogService(LostDogService lostDogService) {
        this.lostDogService = lostDogService;
    }

    public TimelineResponse page(UUID ownerId, UUID petId, LocalDate today, TimelineFilter filter, String before) {
        this.clientDateValidator.validate(today);
        this.petService.getOwnedPetProfile(ownerId, petId);

        int pageSize = PAGE_SIZE;
        TimelineCursor cursor = before == null || before.isBlank() ? TimelineCursor.start(today) : TimelineCursor.parse(before);

        List<TimelineItem> merged = new ArrayList<>();
        merged.addAll(this.logItems(petId, filter, cursor, pageSize));
        merged.addAll(this.activityItems(petId, filter, cursor, pageSize));
        merged.addAll(this.careItems(petId, filter, cursor, pageSize));
        merged.addAll(this.foodChangeItems(petId, filter, cursor, pageSize));
        merged.addAll(this.visitItems(petId, filter, cursor, pageSize));
        merged.addAll(this.lostItems(petId, filter, cursor, pageSize));
        merged.addAll(this.checkItems(petId, filter, cursor, pageSize));
        merged.sort(TimelineItem.NEWEST_FIRST);

        boolean hasMore = merged.size() > pageSize;
        List<TimelineItem> page = hasMore ? merged.subList(0, pageSize) : merged;
        String nextCursor = hasMore ? page.get(page.size() - 1).cursor().format() : null;
        return new TimelineResponse(page.stream().map(TimelineItemDto::of).toList(), nextCursor);
    }

    private List<TimelineItem> checkItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        if (!filter.includesChecks()) return List.of();
        return this.triageApi.checksBetween(petId, LocalDate.EPOCH, cursor.localDate()).stream()
                .map(TimelineItem::of)
                .filter(item -> item.localDate().isBefore(cursor.localDate())
                        || (item.localDate().equals(cursor.localDate()) && (item.minuteOfDay() < cursor.minuteOfDay()
                        || (item.minuteOfDay() == cursor.minuteOfDay() && (item.kind().getOrder() < cursor.kind().getOrder()
                        || (item.kind() == cursor.kind() && item.id().compareTo(cursor.id()) < 0))))))
                .limit(limit + 1L).toList();
    }

    private List<TimelineItem> logItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        LogCursor logCursor = new LogCursor(cursor.localDate(), cursor.minuteOfDay(), cursor.id());
        return this.logService.timelinePage(petId, filter.logTypes(), filter.includesFlaggedToilet(), logCursor, limit).stream()
                .map(TimelineItem::of)
                .toList();
    }

    private List<TimelineItem> activityItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        if (!filter.includesActivities()) {
            return List.of();
        }
        boolean includeWholeDay = cursor.minuteOfDay() > TimelineItem.NO_TIME || cursor.kind() == TimelineKind.LOG;
        UUID idBefore = cursor.kind() == TimelineKind.ACTIVITY ? cursor.id() : ZERO_ID;
        return this.activityService.timelinePage(petId, cursor.localDate(), includeWholeDay, idBefore, limit).stream()
                .map(TimelineItem::of)
                .toList();
    }

    private List<TimelineItem> careItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        if (!filter.includesCare()) {
            return List.of();
        }
        boolean includeWholeDay = cursor.minuteOfDay() > TimelineItem.NO_TIME || cursor.kind() == TimelineKind.LOG
                || cursor.kind() == TimelineKind.ACTIVITY;
        UUID idBefore = cursor.kind() == TimelineKind.CARE ? cursor.id() : ZERO_ID;
        return this.careService.timelinePage(petId, cursor.localDate(), includeWholeDay, idBefore, limit).stream()
                .map(TimelineItem::of)
                .toList();
    }

    /** Food changes have a day but no time; they come after the care events of the same day. */
    private List<TimelineItem> foodChangeItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        if (!filter.includesFoodChanges()) {
            return List.of();
        }
        boolean includeWholeDay = cursor.kind() != TimelineKind.FOOD || cursor.minuteOfDay() > TimelineItem.NO_TIME;
        UUID idBefore = cursor.kind() == TimelineKind.FOOD ? cursor.id() : ZERO_ID;
        return this.nutritionApi.changes(petId).stream()
                .filter(event -> event.date().isBefore(cursor.localDate())
                        || (event.date().equals(cursor.localDate()) && (includeWholeDay || event.id().compareTo(idBefore) < 0)))
                .limit(limit + 1L)
                .map(TimelineItem::of)
                .toList();
    }

    /** Visits sort at the start of their day, after the logs and before the untimed items. */
    private List<TimelineItem> visitItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        if (!filter.includesVisits()) {
            return List.of();
        }
        return this.medicalApi.visitsBetween(petId, LocalDate.EPOCH, cursor.localDate()).stream()
                .filter(visit -> this.isBefore(visit, cursor))
                .limit(limit + 1L)
                .map(TimelineItem::of)
                .toList();
    }

    private boolean isBefore(MedicalApi.VisitSnapshot visit, TimelineCursor cursor) {
        if (visit.visitedOn().isBefore(cursor.localDate())) {
            return true;
        }
        if (TimelineItem.VISIT_MINUTE != cursor.minuteOfDay()) {
            return TimelineItem.VISIT_MINUTE < cursor.minuteOfDay();
        }
        if (cursor.kind() != TimelineKind.VISIT) {
            return TimelineKind.VISIT.getOrder() < cursor.kind().getOrder();
        }
        return visit.id().compareTo(cursor.id()) < 0;
    }

    private List<TimelineItem> lostItems(UUID petId, TimelineFilter filter, TimelineCursor cursor, int limit) {
        if (!filter.includesLostReports()) return List.of();
        return this.lostDogService.timeline(petId, cursor.localDate()).stream()
                .map(TimelineItem::of)
                .filter(item -> item.localDate().isBefore(cursor.localDate())
                        || (item.localDate().equals(cursor.localDate()) && (item.minuteOfDay() < cursor.minuteOfDay()
                        || (item.minuteOfDay() == cursor.minuteOfDay() && (item.kind().getOrder() < cursor.kind().getOrder()
                        || (item.kind() == cursor.kind() && item.id().compareTo(cursor.id()) < 0))))))
                .limit(limit + 1L)
                .toList();
    }
}
