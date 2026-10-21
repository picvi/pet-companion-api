package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.dayevent.domain.DayEventType;
import com.picvi.pet_companion_service.dayevent.domain.EventOccurrence;
import com.picvi.pet_companion_service.dayevent.domain.Recurrence;
import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.FocusType;
import com.picvi.pet_companion_service.development.dto.ActivityRefDto;
import com.picvi.pet_companion_service.development.dto.FocusDto;
import com.picvi.pet_companion_service.development.dto.TodayResponse;
import com.picvi.pet_companion_service.dayevent.service.DayEventService;
import com.picvi.pet_companion_service.development.service.TodayService;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import com.picvi.pet_companion_service.rhythm.domain.BlockKind;
import com.picvi.pet_companion_service.rhythm.domain.DayPlanner;
import com.picvi.pet_companion_service.rhythm.domain.DefaultDayProposer;
import com.picvi.pet_companion_service.rhythm.domain.PlanFlag;
import com.picvi.pet_companion_service.rhythm.domain.RhythmNotices;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import com.picvi.pet_companion_service.rhythm.domain.RhythmStatus;
import com.picvi.pet_companion_service.rhythm.dto.RhythmDayResponse;
import com.picvi.pet_companion_service.rhythm.entity.PetRhythmSettings;
import com.picvi.pet_companion_service.rhythm.repository.PetRhythmSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS;
import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS_SMALL;
import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.TWENTY_WEEKS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RhythmServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    private static final ActivityRefDto SIT = new ActivityRefDto("SIT", ActivityKind.SKILL, ActivityCategory.values()[0], 1,
            ActivityCategory.values()[0].group(), false);
    private static final TodayResponse TODAY_PLAN = new TodayResponse(TODAY, "Mila", 11, null,
            new FocusDto(FocusType.ACTIVITY, SIT, null, false), List.of(), false, false);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private RhythmRuleLoader ruleLoader;
    @Mock
    private PetRhythmSettingsRepository settingsRepository;
    @Mock
    private PetLogService logService;
    @Mock
    private TodayService todayService;
    @Mock
    private DayEventService eventService;
    @Mock
    private com.picvi.pet_companion_service.nutrition.api.NutritionApi nutritionApi;

    private RhythmService service;
    private PetRhythmSettings settings;

    @BeforeEach
    void setUp() {
        this.service = new RhythmService(this.clientDateValidator, this.petService, this.ruleLoader,
                new RhythmRuleResolver(), this.settingsRepository, new DefaultDayProposer(), new DayPlanner(), this.logService, this.todayService, new PlayProposer(), this.eventService, new RhythmNotices(), this.nutritionApi);
    }

    @Test
    void buildsTheDayFromTheSavedSettings() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));

        RhythmDayResponse day = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(day.status()).isEqualTo(RhythmStatus.READY);
        assertThat(day.ageWeeks()).isEqualTo(11);
        assertThat(day.blocks()).extracting("kind").contains(BlockKind.MEAL, BlockKind.NAP, BlockKind.PLAY);
        assertThat(day.nightTrips()).hasSize(1);
        assertThat(day.suggestedSettings()).isNull();
        assertThat(day.playProposal()).isEqualTo(SIT);
    }

    @Test
    void replansTheDayAroundTheLogsOfThatDay() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));
        PetLogDto woke = new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), LogType.WAKE, null, null, TODAY, 400, java.time.OffsetDateTime.parse("2026-10-14T06:40+02:00"), 120, null, null, List.of(), null, null, null, null, null, null,
                UUID.randomUUID(), null, null);
        when(this.logService.logsOn(PET_ID, TODAY)).thenReturn(List.of(woke));

        RhythmDayResponse day = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(day.logs()).containsExactly(woke);
        assertThat(day.blocks().getFirst().endMinute()).isEqualTo(400);
    }

    @Test
    void weightAndNoteLogsDoNotChangeThePlanOrTheLogList() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));
        PetLogDto woke = new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), LogType.WAKE, null, null, TODAY, 400, java.time.OffsetDateTime.parse("2026-10-14T06:40+02:00"), 120, null, null, List.of(), null, null, null, null, null, null,
                UUID.randomUUID(), null, null);
        PetLogDto weight = new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), LogType.WEIGHT, null, null, TODAY, 500, java.time.OffsetDateTime.parse("2026-10-14T08:20+02:00"), 120, null, null, List.of(), 2300, null, null, null, null, null,
                UUID.randomUUID(), null, null);
        PetLogDto note = new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), LogType.NOTE, null, null, TODAY, 520, java.time.OffsetDateTime.parse("2026-10-14T08:40+02:00"), 120, null, null, List.of(), null, null, null, null, null, "Barked",
                UUID.randomUUID(), null, null);
        when(this.logService.logsOn(PET_ID, TODAY)).thenReturn(List.of(woke, weight, note));
        RhythmDayResponse withExtras = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        when(this.logService.logsOn(PET_ID, TODAY)).thenReturn(List.of(woke));
        RhythmDayResponse without = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(withExtras.logs()).containsExactly(woke);
        assertThat(withExtras.blocks()).isEqualTo(without.blocks());
        assertThat(withExtras.logs()).isEqualTo(without.logs());
    }

    @Test
    void plansTheDayAroundEventsOfThatDay() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));
        EventOccurrence vet = new EventOccurrence(UUID.randomUUID(), DayEventType.VET, null, 600, 60, Recurrence.NONE, true);
        EventOccurrence guests = new EventOccurrence(UUID.randomUUID(), DayEventType.GUESTS, null, 1000, 60, Recurrence.NONE, false);
        when(this.eventService.occurrencesOn(PET_ID, TODAY)).thenReturn(List.of(vet, guests));

        RhythmDayResponse day = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(day.events()).hasSize(2);
        assertThat(day.blocks()).extracting("kind").contains(BlockKind.EVENT);
        assertThat(day.blocks().stream().filter(block -> block.kind() == BlockKind.EVENT)).hasSize(1);
    }

    @Test
    void showsABandNoteWhenThePuppyHasMovedToAnotherBand() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));
        this.settings.setBandNoteSeenFromWeeks(8);

        assertThat(this.service.getDay(OWNER_ID, PET_ID, TODAY).bandNoteFromWeeks()).isEqualTo(10);
    }

    @Test
    void asksForTheCheckinTwoWeeksAfterSetup() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));
        ReflectionTestUtils.setField(this.settings, "createdOn", TODAY.minusDays(14));

        RhythmDayResponse day = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(day.checkinDue()).isTrue();
        assertThat(day.bandNoteFromWeeks()).isNull();
    }

    @Test
    void usesTheRuleForThePetsSize() {
        this.givenPetAgedWeeks(11, SizeCategory.SMALL);
        this.givenSettings(420, 1350, List.of(435, 720, 930, 1110));

        assertThat(this.service.getDay(OWNER_ID, PET_ID, TODAY).flags()).contains(PlanFlag.MEAL_SAFETY_NOTE);
    }

    @Test
    void asksForSetupWhenTheOwnerHasNotChosenTheirTimesYet() {
        this.givenPetAgedWeeks(11, SizeCategory.MEDIUM);
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.empty());

        RhythmDayResponse day = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(day.status()).isEqualTo(RhythmStatus.SETUP_NEEDED);
        assertThat(day.suggestedSettings().mealMinutes()).hasSize(4);
        assertThat(day.rule()).isNotNull();
        assertThat(day.blocks()).isEmpty();
    }

    @Test
    void saysTheRhythmIsNotAvailableOnceThePuppyIsPastTheLastBand() {
        this.givenPetAgedWeeks(30, SizeCategory.MEDIUM);

        RhythmDayResponse day = this.service.getDay(OWNER_ID, PET_ID, TODAY);

        assertThat(day.status()).isEqualTo(RhythmStatus.NOT_AVAILABLE_FOR_AGE);
        assertThat(day.rule()).isNull();
    }

    @Test
    void rejectsADateBeforeThePuppyWasBorn() {
        LocalDate birthDate = TODAY.minusWeeks(11);
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID))
                .thenReturn(new PetProfile(PET_ID, "Mila", birthDate, SizeCategory.MEDIUM, birthDate, VaccinationStatus.UNKNOWN, null, 0));

        assertThatThrownBy(() -> this.service.getDay(OWNER_ID, PET_ID, TODAY.minusWeeks(12))).isInstanceOf(InvalidDate.class);
    }

    @Test
    void rejectsADateTheClientValidatorRefuses() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.getDay(OWNER_ID, PET_ID, TODAY)).isInstanceOf(InvalidDate.class);
    }

    @Test
    void doesNotShowAnotherOwnersPet() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.getDay(OWNER_ID, PET_ID, TODAY)).isInstanceOf(PetNotFound.class);
    }

    private void givenPetAgedWeeks(int weeks, SizeCategory size) {
        LocalDate birthDate = TODAY.minusWeeks(weeks);
        when(this.ruleLoader.loadAll()).thenReturn(List.of(ELEVEN_WEEKS, ELEVEN_WEEKS_SMALL, TWENTY_WEEKS));
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID))
                .thenReturn(new PetProfile(PET_ID, "Mila", birthDate, size, birthDate, VaccinationStatus.UNKNOWN, null, 0));
    }

    private void givenSettings(int wake, int bed, List<Integer> meals) {
        PetRhythmSettings settings = new PetRhythmSettings(null, TODAY, 10);
        this.settings = settings;
        settings.setWakeMinute(wake);
        settings.setBedMinute(bed);
        settings.replaceMealMinutes(meals);
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.of(settings));
        when(this.todayService.buildToday(any(), eq(TODAY))).thenReturn(TODAY_PLAN);
    }
}
