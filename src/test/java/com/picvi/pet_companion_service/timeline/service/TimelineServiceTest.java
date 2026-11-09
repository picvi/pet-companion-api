package com.picvi.pet_companion_service.timeline.service;

import com.picvi.pet_companion_service.care.dto.CareTimelineRow;
import com.picvi.pet_companion_service.care.service.CareEventService;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.dto.ActivityTimelineRow;
import com.picvi.pet_companion_service.development.service.ActivityProgressService;
import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;
import com.picvi.pet_companion_service.nutrition.api.FoodChangeEvent;
import com.picvi.pet_companion_service.medical.api.MedicalApi;
import com.picvi.pet_companion_service.lostdog.service.LostDogService;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.domain.LogCursor;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import com.picvi.pet_companion_service.timeline.domain.TimelineFilter;
import com.picvi.pet_companion_service.timeline.dto.TimelineItemDto;
import com.picvi.pet_companion_service.timeline.dto.TimelineResponse;
import com.picvi.pet_companion_service.triage.api.TriageApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimelineServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private PetLogService logService;
    @Mock
    private ActivityProgressService activityService;
    @Mock
    private CareEventService careService;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private MedicalApi medicalApi;
    @Mock
    private TriageApi triageApi;
    @Mock
    private LostDogService lostDogService;

    private TimelineService service;

    @BeforeEach
    void setUp() {
        this.service = new TimelineService(this.clientDateValidator, this.petService, this.logService, this.activityService,
                this.careService, this.nutritionApi, this.medicalApi, this.triageApi);
        lenient().when(this.triageApi.checksBetween(any(), any(), any())).thenReturn(List.of());
        lenient().when(this.lostDogService.timeline(any(), any())).thenReturn(List.of());
        this.service.setLostDogService(this.lostDogService);
    }

    @Test
    void aDayIsOrderedLogThenActivityThenCare() {
        PetLogDto log = log(TODAY, 570, LogType.MEAL, 0);
        ActivityTimelineRow activity = new ActivityTimelineRow(UUID.randomUUID(), TODAY, "SIT", Outcome.LEARNED, null);
        CareTimelineRow care = new CareTimelineRow(UUID.randomUUID(), TODAY, "VACCINE_1");
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of(log));
        when(this.activityService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of(activity));
        when(this.careService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of(care));

        TimelineResponse response = this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, null);

        assertThat(response.items()).extracting(TimelineItemDto::kind).containsExactly("LOG", "ACTIVITY", "CARE");
        assertThat(response.items().get(1).minuteOfDay()).isNull();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void pagingAcrossADayBoundaryReturnsEveryItemExactlyOnce() {
        List<PetLogDto> logs = new ArrayList<>();
        for (int minute = 0; minute < 60; minute++) {
            logs.add(log(TODAY, 300 + minute, LogType.MEAL, 0));
            logs.add(log(TODAY.minusDays(1), 300 + minute, LogType.MEAL, 0));
        }
        ActivityTimelineRow activity = new ActivityTimelineRow(UUID.randomUUID(), TODAY, "SIT", Outcome.LEARNED, null);
        CareTimelineRow todayCare = new CareTimelineRow(UUID.randomUUID(), TODAY, "VACCINE_1");
        CareTimelineRow olderCare = new CareTimelineRow(UUID.randomUUID(), TODAY.minusDays(1), "DEWORMING");
        this.fakeSources(logs, List.of(activity), List.of(todayCare, olderCare));

        List<UUID> seen = new ArrayList<>();
        String cursor = null;
        int pages = 0;
        do {
            TimelineResponse page = this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, cursor);
            assertThat(page.items()).hasSizeLessThanOrEqualTo(50);
            page.items().forEach(item -> seen.add(item.id()));
            cursor = page.nextCursor();
            pages++;
        } while (cursor != null);

        assertThat(pages).isEqualTo(3);
        assertThat(seen).hasSize(123).doesNotHaveDuplicates();
        assertThat(seen.indexOf(activity.id())).isEqualTo(60);
        assertThat(seen.indexOf(todayCare.id())).isEqualTo(61);
        assertThat(seen.indexOf(olderCare.id())).isEqualTo(122);
    }

    @Test
    void healthIncludesFlaggedToiletAndCareButNoActivities() {
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());
        when(this.careService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());

        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.HEALTH, null);

        ArgumentCaptor<Set<LogType>> types = ArgumentCaptor.forClass(Set.class);
        verify(this.logService).timelinePage(eq(PET_ID), types.capture(), eq(true), any(), eq(50));
        assertThat(types.getValue()).containsExactlyInAnyOrder(LogType.SYMPTOM, LogType.MEDICINE, LogType.WEIGHT);
        verify(this.careService).timelinePage(eq(PET_ID), any(), anyBoolean(), any(), eq(50));
        verifyNoInteractions(this.activityService);
    }

    @Test
    void nonHealthFiltersDoNotAskForFlaggedToilet() {
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());

        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.TOILET, null);

        verify(this.logService).timelinePage(eq(PET_ID), eq(Set.of(LogType.TOILET)), eq(false), any(), eq(50));
    }

    @Test
    void foodNeverCallsActivityOrCareServices() {
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());

        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.FOOD, null);

        verifyNoInteractions(this.activityService, this.careService);
    }

    @Test
    void anActivityCursorSkipsActivitiesAlreadyShownButKeepsTheRestOfTheDayForCare() {
        UUID activityId = UUID.randomUUID();
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());
        when(this.activityService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());
        when(this.careService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());

        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, "2026-10-14,-1,ACTIVITY," + activityId);

        verify(this.activityService).timelinePage(PET_ID, TODAY, false, activityId, 50);
        verify(this.careService).timelinePage(eq(PET_ID), eq(TODAY), eq(true), any(), eq(50));
    }

    @Test
    void aCareCursorKeepsNothingElseOfThatDayForActivities() {
        UUID careId = UUID.randomUUID();
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());
        when(this.activityService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());
        when(this.careService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());

        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, "2026-10-14,-1,CARE," + careId);

        verify(this.activityService).timelinePage(eq(PET_ID), eq(TODAY), eq(false), any(), eq(50));
        verify(this.careService).timelinePage(PET_ID, TODAY, false, careId, 50);
    }

    @Test
    void rejectsAGarbageCursor() {
        assertThatThrownBy(() -> this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, "nonsense"))
                .isInstanceOf(InvalidCursor.class);
        verify(this.logService, never()).timelinePage(any(), any(), anyBoolean(), any(), anyInt());
    }


    @Test
    void foodChangesAreListedAfterTheCareEventsOfTheSameDay() {
        CareTimelineRow care = new CareTimelineRow(UUID.randomUUID(), TODAY, "VACCINE_1");
        FoodChangeEvent started = new FoodChangeEvent(UUID.randomUUID(), FoodChangeEvent.Kind.STARTED, TODAY, UUID.randomUUID());
        this.fakeSources(List.of(), List.of(), List.of(care));
        when(this.nutritionApi.changes(PET_ID)).thenReturn(List.of(started));

        TimelineResponse response = this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, null);

        assertThat(response.items()).extracting(TimelineItemDto::kind).containsExactly("CARE", "FOOD");
        assertThat(response.items().get(1).food().kind()).isEqualTo(FoodChangeEvent.Kind.STARTED);
        assertThat(response.items().get(1).minuteOfDay()).isNull();
    }

    @Test
    void foodChangesAreFilteredOutOfTheToiletView() {
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of());

        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.TOILET, null);

        verify(this.nutritionApi, never()).changes(any());
    }

    @Test
    void pagingPastAFoodChangeDoesNotRepeatIt() {
        FoodChangeEvent newer = new FoodChangeEvent(UUID.randomUUID(), FoodChangeEvent.Kind.SLOWED, TODAY, UUID.randomUUID());
        FoodChangeEvent older = new FoodChangeEvent(UUID.randomUUID(), FoodChangeEvent.Kind.STARTED, TODAY.minusDays(3), UUID.randomUUID());
        this.fakeSources(List.of(), List.of(), List.of());
        when(this.nutritionApi.changes(PET_ID)).thenReturn(List.of(newer, older));

        TimelineResponse next = this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, "2026-10-14,-1,FOOD," + newer.id());

        assertThat(next.items()).extracting(TimelineItemDto::id).containsExactly(older.id());
    }

    @Test
    void aVisitSitsAfterTheLogsOfItsDayAndOnlyShowsInTheAllView() {
        PetLogDto log = log(TODAY, 570, LogType.MEAL, 0);
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenReturn(List.of(log));
        MedicalApi.VisitSnapshot visit = new MedicalApi.VisitSnapshot(UUID.randomUUID(), TODAY,
                List.of(VisitOutcome.TREATMENT), "VetCare");
        when(this.medicalApi.visitsBetween(eq(PET_ID), any(), any())).thenReturn(List.of(visit));

        TimelineResponse all = this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, null);
        this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.HEALTH, null);

        assertThat(all.items()).extracting(TimelineItemDto::kind).containsExactly("LOG", "VISIT");
        assertThat(all.items().get(1).minuteOfDay()).isZero();
        assertThat(all.items().get(1).visit().clinic()).isEqualTo("VetCare");
        verify(this.medicalApi).visitsBetween(any(), any(), any());
    }

    @Test
    void pagingPastAVisitDoesNotRepeatIt() {
        this.fakeSources(List.of(), List.of(), List.of());
        MedicalApi.VisitSnapshot newer = new MedicalApi.VisitSnapshot(UUID.randomUUID(), TODAY, List.of(), null);
        MedicalApi.VisitSnapshot older = new MedicalApi.VisitSnapshot(UUID.randomUUID(), TODAY.minusDays(2), List.of(), null);
        when(this.medicalApi.visitsBetween(eq(PET_ID), any(), any())).thenReturn(List.of(newer, older));

        TimelineResponse next = this.service.page(OWNER_ID, PET_ID, TODAY, TimelineFilter.ALL, "2026-10-14,0,VISIT," + newer.id());

        assertThat(next.items()).extracting(TimelineItemDto::id).containsExactly(older.id());
    }

    private void fakeSources(List<PetLogDto> logs, List<ActivityTimelineRow> activities, List<CareTimelineRow> careEvents) {
        when(this.logService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenAnswer(call -> {
            LogCursor cursor = call.getArgument(3);
            int limit = call.getArgument(4);
            return logs.stream()
                    .filter(log -> log.localDate().isBefore(cursor.localDate())
                            || (log.localDate().equals(cursor.localDate()) && log.minuteOfDay() < cursor.minuteOfDay())
                            || (log.localDate().equals(cursor.localDate()) && log.minuteOfDay() == cursor.minuteOfDay()
                            && log.id().compareTo(cursor.id()) < 0))
                    .sorted(Comparator.comparing(PetLogDto::localDate).thenComparing(PetLogDto::minuteOfDay).reversed())
                    .limit(limit + 1L)
                    .toList();
        });
        when(this.activityService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenAnswer(call -> {
            LocalDate date = call.getArgument(1);
            boolean wholeDay = call.getArgument(2);
            UUID idBefore = call.getArgument(3);
            int limit = call.getArgument(4);
            return activities.stream()
                    .filter(a -> a.performedOn().isBefore(date)
                            || (a.performedOn().equals(date) && (wholeDay || a.id().compareTo(idBefore) < 0)))
                    .sorted(Comparator.comparing(ActivityTimelineRow::performedOn).reversed())
                    .limit(limit + 1L)
                    .toList();
        });
        when(this.careService.timelinePage(any(), any(), anyBoolean(), any(), anyInt())).thenAnswer(call -> {
            LocalDate date = call.getArgument(1);
            boolean wholeDay = call.getArgument(2);
            UUID idBefore = call.getArgument(3);
            int limit = call.getArgument(4);
            return careEvents.stream()
                    .filter(c -> c.performedOn().isBefore(date)
                            || (c.performedOn().equals(date) && (wholeDay || c.id().compareTo(idBefore) < 0)))
                    .sorted(Comparator.comparing(CareTimelineRow::performedOn).reversed())
                    .limit(limit + 1L)
                    .toList();
        });
    }

    private static PetLogDto log(LocalDate date, int minute, LogType type, int weight) {
        OffsetDateTime occurredAt = date.atStartOfDay().plusMinutes(minute).atOffset(java.time.ZoneOffset.ofHours(2));
        return new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), type, null, null, date, minute, occurredAt, 120, null, null, List.of(), null, null, null, null, null, null, UUID.randomUUID(), null, null);
    }
}
