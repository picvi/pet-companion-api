package com.picvi.pet_companion_service.petlog.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.exception.petlog.InvalidCursor;
import com.picvi.pet_companion_service.exception.petlog.InvalidLog;
import com.picvi.pet_companion_service.exception.petlog.LogNotFound;
import com.picvi.pet_companion_service.exception.petlog.LogTimeInFuture;
import com.picvi.pet_companion_service.exception.nutrition.FoodNotActive;
import com.picvi.pet_companion_service.nutrition.api.ActiveFood;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.petlog.api.CourseDoseCheck;
import com.picvi.pet_companion_service.petlog.domain.CourseDose;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.domain.LogChange;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogEdit;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.petlog.domain.OccurredAtCalculator;
import com.picvi.pet_companion_service.petlog.domain.PetLogRules;
import com.picvi.pet_companion_service.petlog.domain.RecentLogs;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import com.picvi.pet_companion_service.petlog.dto.PetLogHistoryResponse;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.petlog.entity.PetLogChangeRow;
import com.picvi.pet_companion_service.petlog.repository.PetLogChangeRepository;
import com.picvi.pet_companion_service.petlog.repository.PetLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetLogServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final UUID CLIENT_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final PetProfile PET = new PetProfile(PET_ID, "Mila", TODAY.minusWeeks(11), SizeCategory.SMALL,
            TODAY.minusWeeks(11), VaccinationStatus.UNKNOWN, null, 0);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T10:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private PetLogRepository logRepository;
    @Mock
    private PetLogChangeRepository changeRepository;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private CourseDoseCheck courseDoseCheck;
    @Mock
    private ApplicationEventPublisher events;

    private PetLogService service;

    @BeforeEach
    void setUp() {
        this.service = new PetLogService(this.clientDateValidator, this.petService, new PetLogRules(), this.logRepository,
                this.changeRepository, new OccurredAtCalculator(ZoneId.of("Europe/Warsaw")), CLOCK, new RecentLogs(), this.nutritionApi, this.courseDoseCheck, this.events);
    }

    @Test
    void savesANewLogWithOccurredAtAndRecordsTheChange() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));

        PetLogService.LogResult result = this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.TOILET, LogDetail.PEE, LogValues.none(), TODAY, 570, 120), TODAY);

        ArgumentCaptor<PetLog> saved = ArgumentCaptor.forClass(PetLog.class);
        verify(this.logRepository).save(saved.capture());
        assertThat(saved.getValue().getLoggedBy()).isEqualTo(OWNER_ID);
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(OffsetDateTime.parse("2026-10-14T09:30+02:00"));
        assertThat(saved.getValue().getUtcOffsetMinutes()).isEqualTo(120);
        assertThat(result.created()).isTrue();
        assertThat(result.log().type()).isEqualTo(LogType.TOILET);
        this.assertChangesRecorded(LogChange.CREATED);
    }

    @Test
    void sendingTheSameLogTwiceReturnsTheFirstOneWithoutSavingAgain() {
        this.givenOwnedPet();
        PetLog first = this.existingLog(LogType.MEAL, LogDetail.ATE_ALL, new LogValues(null, null, null, 40, null, null, null, null, null, null));
        when(this.logRepository.findByPetIdAndClientId(PET_ID, CLIENT_ID)).thenReturn(Optional.of(first));

        PetLogService.LogResult result = this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.MEAL, LogDetail.ATE_ALL, new LogValues(null, null, null, 40, null, null, null, null, null, null), TODAY, 435, null), TODAY);

        assertThat(result.created()).isFalse();
        verify(this.logRepository, never()).save(any());
        verify(this.changeRepository, never()).save(any());
    }

    @Test
    void anOldStyleLogGetsTheWarsawOffset() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));

        this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.MEAL, LogDetail.LEFT_SOME, LogValues.none(), TODAY, 435, null), TODAY);

        ArgumentCaptor<PetLog> saved = ArgumentCaptor.forClass(PetLog.class);
        verify(this.logRepository).save(saved.capture());
        assertThat(saved.getValue().getUtcOffsetMinutes()).isEqualTo(120);
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(OffsetDateTime.parse("2026-10-14T07:15+02:00"));
    }

    @Test
    void rejectsInvalidValuesWithoutSavingAnything() {
        this.givenOwnedPet();
        this.givenNoExistingLog();

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.TOILET, null, LogValues.none(), TODAY, 540, null), TODAY)).isInstanceOf(InvalidLog.class);
        verify(this.logRepository, never()).save(any());
        verify(this.changeRepository, never()).save(any());
    }

    @Test
    void rejectsALogDatedAfterToday() {
        this.givenOwnedPet();
        this.givenNoExistingLog();

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.WAKE, null, LogValues.none(), TODAY.plusDays(1), 540, null), TODAY)).isInstanceOf(LogTimeInFuture.class);
    }

    @Test
    void rejectsALogDatedBeforeThePuppyWasBorn() {
        this.givenOwnedPet();
        this.givenNoExistingLog();

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.WAKE, null, LogValues.none(), TODAY.minusWeeks(12), 540, null), TODAY)).isInstanceOf(InvalidDate.class);
    }

    @Test
    void rejectsADateTheClientValidatorRefuses() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.WAKE, null, LogValues.none(), TODAY, 540, null), TODAY)).isInstanceOf(InvalidDate.class);
    }

    @Test
    void doesNotLogForAnotherOwnersPet() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.WAKE, null, LogValues.none(), TODAY, 540, null), TODAY)).isInstanceOf(PetNotFound.class);
    }

    @Test
    void updateOfAnotherPetsLogFailsAndSavesNothing() {
        this.givenOwnedPet();
        UUID logId = UUID.randomUUID();
        when(this.logRepository.findByIdAndPetId(logId, PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, logId,
                new LogEdit(null, LogValues.none(), TODAY, 540, null), TODAY)).isInstanceOf(LogNotFound.class);
        verify(this.changeRepository, never()).save(any());
    }

    @Test
    void updateKeepsTypeSourceAndClientIdAndReplacesValues() {
        this.givenOwnedPet();
        PetLog log = this.existingLog(LogType.TOILET, LogDetail.POO,
                new LogValues(null, 4, Set.of(LogFlag.BLOOD), null, null, null, null, null, null, null));
        UUID logId = UUID.randomUUID();
        ReflectionTestUtils.setField(log, "id", logId);
        when(this.logRepository.findByIdAndPetId(logId, PET_ID)).thenReturn(Optional.of(log));

        LogValues edited = new LogValues(true, 5, Set.of(LogFlag.MUCUS), null, null, null, null, null, null, "soft");
        var dto = this.service.update(OWNER_ID, PET_ID, logId, new LogEdit(LogDetail.BOTH, edited, TODAY, 600, 60), TODAY);

        assertThat(log.getType()).isEqualTo(LogType.TOILET);
        assertThat(log.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(log.getDetail()).isEqualTo(LogDetail.BOTH);
        assertThat(log.getStoolScore()).isEqualTo(5);
        assertThat(log.getFlags()).containsExactly(LogFlag.MUCUS);
        assertThat(log.getNote()).isEqualTo("soft");
        assertThat(log.getMinuteOfDay()).isEqualTo(600);
        assertThat(log.getUtcOffsetMinutes()).isEqualTo(60);
        assertThat(log.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-14T10:00:00Z"));
        assertThat(dto.flags()).containsExactly(LogFlag.MUCUS);
        this.assertChangesRecorded(LogChange.UPDATED);
    }

    @Test
    void updateValidatesAgainstTheTypeOfTheStoredLog() {
        this.givenOwnedPet();
        PetLog log = this.existingLog(LogType.WEIGHT, null,
                new LogValues(null, null, null, null, 2300, null, null, null, null, null));
        UUID logId = UUID.randomUUID();
        when(this.logRepository.findByIdAndPetId(logId, PET_ID)).thenReturn(Optional.of(log));

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, logId,
                new LogEdit(null, LogValues.none(), TODAY, 540, null), TODAY)).isInstanceOf(InvalidLog.class);
        verify(this.changeRepository, never()).save(any());
    }

    @Test
    void listsTheLogsOfADayInTimeOrder() {
        this.givenOwnedPet();
        PetLog log = this.existingLog(LogType.WAKE, null, LogValues.none());
        ReflectionTestUtils.setField(log, "id", UUID.randomUUID());
        when(this.logRepository.findAllByPetIdAndLocalDateOrderByMinuteOfDayAsc(PET_ID, TODAY)).thenReturn(List.of(log));

        assertThat(this.service.list(OWNER_ID, PET_ID, TODAY).items()).extracting("minuteOfDay").containsExactly(420);
    }

    @Test
    void undoRecordsTheDeletionAndRemovesTheLog() {
        this.givenOwnedPet();
        PetLog log = this.existingLog(LogType.WAKE, null, LogValues.none());
        UUID logId = UUID.randomUUID();
        when(this.logRepository.findByIdAndPetId(logId, PET_ID)).thenReturn(Optional.of(log));

        this.service.undo(OWNER_ID, PET_ID, logId, TODAY);

        verify(this.logRepository).delete(log);
        this.assertChangesRecorded(LogChange.DELETED);
    }

    @Test
    void undoFailsForAnUnknownLog() {
        this.givenOwnedPet();
        UUID logId = UUID.randomUUID();
        when(this.logRepository.findByIdAndPetId(logId, PET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.undo(OWNER_ID, PET_ID, logId, TODAY)).isInstanceOf(LogNotFound.class);
    }

    @Test
    void historyReturnsAPageAndACursorWhenThereIsMore() {
        this.givenOwnedPet();
        List<PetLog> rows = this.logsAt(51);
        when(this.logRepository.findPageBefore(any(), any(), any(), anyInt(), any(), any())).thenReturn(rows);

        PetLogHistoryResponse response = this.service.history(OWNER_ID, PET_ID, TODAY, null, null);

        assertThat(response.items()).hasSize(50);
        PetLog last = rows.get(49);
        assertThat(response.nextCursor()).isEqualTo(last.getLocalDate() + "," + last.getMinuteOfDay() + "," + last.getId());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(this.logRepository).findPageBefore(eq(PET_ID), eq(EnumSet.allOf(LogType.class)), eq(TODAY.plusDays(2)),
                eq(0), eq(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff")), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(51);
    }

    @Test
    void historyLastPageHasNoCursor() {
        this.givenOwnedPet();
        when(this.logRepository.findPageBefore(any(), any(), any(), anyInt(), any(), any())).thenReturn(this.logsAt(20));

        PetLogHistoryResponse response = this.service.history(OWNER_ID, PET_ID, TODAY, Set.of(), "");

        assertThat(response.items()).hasSize(20);
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    void historyPassesOnlyTheRequestedTypes() {
        this.givenOwnedPet();
        when(this.logRepository.findPageBefore(any(), any(), any(), anyInt(), any(), any())).thenReturn(List.of());
        UUID id = UUID.randomUUID();

        this.service.history(OWNER_ID, PET_ID, TODAY, Set.of(LogType.WEIGHT), "2026-10-10,300," + id);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(this.logRepository).findPageBefore(eq(PET_ID), eq(Set.of(LogType.WEIGHT)), eq(LocalDate.of(2026, 10, 10)),
                eq(300), eq(id), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(51);
    }

    @Test
    void historyRejectsAGarbageCursor() {
        this.givenOwnedPet();

        assertThatThrownBy(() -> this.service.history(OWNER_ID, PET_ID, TODAY, null, "garbage"))
                .isInstanceOf(InvalidCursor.class);
    }


    private List<PetLog> logsAt(int count) {
        List<PetLog> logs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PetLog log = this.existingLog(LogType.WAKE, null, LogValues.none());
            ReflectionTestUtils.setField(log, "id", UUID.randomUUID());
            logs.add(log);
        }
        return logs;
    }

    private void assertChangesRecorded(LogChange expected) {
        ArgumentCaptor<PetLogChangeRow> change = ArgumentCaptor.forClass(PetLogChangeRow.class);
        verify(this.changeRepository).save(change.capture());
        assertThat(change.getValue().getChange()).isEqualTo(expected);
        assertThat(change.getValue().getPetId()).isEqualTo(PET_ID);
        assertThat(change.getValue().getChangedAt()).isEqualTo(OffsetDateTime.parse("2026-10-14T10:00:00Z"));
    }

    @Test
    void aMixedMealSavesOneLogWithAFoodRowPerFood() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        UUID oldFood = UUID.randomUUID();
        UUID newFood = UUID.randomUUID();
        when(this.nutritionApi.activeFoods(PET_ID, TODAY)).thenReturn(List.of(
                new ActiveFood(oldFood, UUID.randomUUID(), FoodKind.MAIN), new ActiveFood(newFood, UUID.randomUUID(), FoodKind.MAIN)));
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));
        LogValues values = new LogValues(null, null, null, null, null, null, null, null, null, null,
                List.of(new LogFood(oldFood), new LogFood(newFood)));

        PetLogService.LogResult result = this.service.log(OWNER_ID, PET_ID, newLog(LogType.MEAL, LogDetail.ATE_ALL, values, TODAY, 480, 120), TODAY);

        assertThat(result.log().foods()).extracting(LogFood::petFoodId).containsExactlyInAnyOrder(oldFood, newFood);
    }

    @Test
    void aFoodThatIsNotTheirsOrNotGivenThatDayIsRejected() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.nutritionApi.activeFoods(PET_ID, TODAY)).thenReturn(List.of());
        LogValues values = new LogValues(null, null, null, null, null, null, null, null, null, null,
                List.of(new LogFood(UUID.randomUUID())));

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID, newLog(LogType.MEAL, LogDetail.ATE_ALL, values, TODAY, 480, 120), TODAY))
                .isInstanceOf(FoodNotActive.class);
        verify(this.logRepository, never()).save(any());
    }

    @Test
    void aMealWithoutFoodsNeverAsksNutrition() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));

        this.service.log(OWNER_ID, PET_ID, newLog(LogType.MEAL, LogDetail.ATE_ALL, LogValues.none(), TODAY, 480, 120), TODAY);

        verify(this.nutritionApi, never()).activeFoods(any(), any());
    }

    @Test
    void aMedicineAnsweringAPlannedDoseIsLinkedToItsCourse() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        UUID course = UUID.randomUUID();
        CourseDose dose = new CourseDose(course, TODAY, 480);
        when(this.logRepository.findByCourseIdAndDoseOnAndDoseMinute(course, TODAY, 480)).thenReturn(Optional.empty());
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));

        PetLogService.LogResult result = this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.MEDICINE, null, medicineValues(), TODAY, 480, 120, dose), TODAY);

        verify(this.courseDoseCheck).verify(PET_ID, dose);
        assertThat(result.created()).isTrue();
        assertThat(result.log().courseId()).isEqualTo(course);
        assertThat(result.log().doseOn()).isEqualTo(TODAY);
        assertThat(result.log().doseMinute()).isEqualTo(480);
    }

    @Test
    void aPlannedDoseThatWasAlreadyGivenReturnsTheFirstLogWithoutSavingAgain() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        UUID course = UUID.randomUUID();
        PetLog first = this.existingLog(LogType.MEDICINE, null, medicineValues());
        when(this.logRepository.findByCourseIdAndDoseOnAndDoseMinute(course, TODAY, 480)).thenReturn(Optional.of(first));

        PetLogService.LogResult result = this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.MEDICINE, null, medicineValues(), TODAY, 480, 120, new CourseDose(course, TODAY, 480)), TODAY);

        assertThat(result.created()).isFalse();
        verify(this.logRepository, never()).save(any());
    }

    @Test
    void aDoseWithoutAPlannedTimeIsNotLookedUpAndNeverLocks() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));

        this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.MEDICINE, null, medicineValues(), TODAY, 480, 120, new CourseDose(UUID.randomUUID(), null, null)), TODAY);

        verify(this.logRepository, never()).lockDose(any());
        verify(this.logRepository, never()).findByCourseIdAndDoseOnAndDoseMinute(any(), any(), any());
    }

    @Test
    void onlyAMedicineLogCanCarryACourseDose() {
        this.givenOwnedPet();
        this.givenNoExistingLog();

        assertThatThrownBy(() -> this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.WEIGHT, null, new LogValues(null, null, null, null, 2300, null, null, null, null, null), TODAY, 480, 120,
                        new CourseDose(UUID.randomUUID(), null, null)), TODAY))
                .isInstanceOf(InvalidLog.class);

        verify(this.logRepository, never()).save(any());
    }

    private static LogValues medicineValues() {
        return new LogValues(null, null, null, null, null, null, null, "Metronidazole", "1 tablet", null);
    }

    private PetLog existingLog(LogType type, LogDetail detail, LogValues values) {
        return new PetLog(null, CLIENT_ID, type, OWNER_ID, detail, values, TODAY, 420,
                new LoggedAt(OffsetDateTime.parse("2026-10-14T07:00+02:00"), 120), null);
    }

    private static NewLog newLog(LogType type, LogDetail detail, LogValues values, LocalDate localDate, int minute, Integer offset) {
        return newLog(type, detail, values, localDate, minute, offset, null);
    }

    private static NewLog newLog(LogType type, LogDetail detail, LogValues values, LocalDate localDate, int minute, Integer offset,
                                 CourseDose courseDose) {
        return new NewLog(CLIENT_ID, type, detail, values, localDate, minute, offset, courseDose);
    }

    private void givenOwnedPet() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(PET);
    }

    private void givenNoExistingLog() {
        when(this.logRepository.findByPetIdAndClientId(PET_ID, CLIENT_ID)).thenReturn(Optional.empty());
    }

    @Test
    void savingALogPublishesASnapshotForOtherModules() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));
        LogValues values = new LogValues(null, 6, null, null, null, null, null, null, null, null);

        this.service.log(OWNER_ID, PET_ID, newLog(LogType.TOILET, LogDetail.POO, values, TODAY, 570, 120), TODAY);

        ArgumentCaptor<com.picvi.pet_companion_service.petlog.api.PetLogSaved> event = ArgumentCaptor.forClass(com.picvi.pet_companion_service.petlog.api.PetLogSaved.class);
        verify(this.events).publishEvent(event.capture());
        assertThat(event.getValue().petId()).isEqualTo(PET_ID);
        assertThat(event.getValue().type()).isEqualTo(LogType.TOILET);
        assertThat(event.getValue().stoolScore()).isEqualTo(6);
        assertThat(event.getValue().localDate()).isEqualTo(TODAY);
    }

    @Test
    void aMealWithTheIncomingFoodGetsTheNewFoodFlag() {
        this.givenOwnedPet();
        this.givenNoExistingLog();
        when(this.nutritionApi.isNewFoodMeal(PET_ID, TODAY, List.of())).thenReturn(true);
        when(this.logRepository.save(any(PetLog.class))).thenAnswer(call -> call.getArgument(0));

        PetLogService.LogResult result = this.service.log(OWNER_ID, PET_ID,
                newLog(LogType.MEAL, LogDetail.ATE_ALL, LogValues.none(), TODAY, 480, 120), TODAY);

        assertThat(result.log().flags()).containsExactly(com.picvi.pet_companion_service.petlog.domain.LogFlag.NEW_FOOD);
    }
}
