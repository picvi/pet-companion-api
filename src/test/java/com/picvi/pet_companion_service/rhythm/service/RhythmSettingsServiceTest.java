package com.picvi.pet_companion_service.rhythm.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.rhythm.MealsOutOfRange;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.rhythm.domain.DaySettings;
import com.picvi.pet_companion_service.rhythm.domain.RhythmRuleResolver;
import com.picvi.pet_companion_service.rhythm.domain.RhythmSettingsValidator;
import com.picvi.pet_companion_service.rhythm.dto.RhythmDayResponse;
import com.picvi.pet_companion_service.rhythm.entity.PetRhythmSettings;
import com.picvi.pet_companion_service.rhythm.repository.PetRhythmSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RhythmSettingsServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final PetProfile PET = new PetProfile(PET_ID, "Mila", TODAY.minusWeeks(11), SizeCategory.MEDIUM,
            TODAY.minusWeeks(11), VaccinationStatus.UNKNOWN, null, 0);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private RhythmRuleLoader ruleLoader;
    @Mock
    private RhythmSettingsValidator settingsValidator;
    @Mock
    private PetRhythmSettingsRepository settingsRepository;
    @Mock
    private RhythmService rhythmService;

    private RhythmSettingsService service;

    @BeforeEach
    void setUp() {
        this.service = new RhythmSettingsService(this.clientDateValidator, this.petService, this.ruleLoader,
                new RhythmRuleResolver(), this.settingsValidator, new com.picvi.pet_companion_service.rhythm.domain.DefaultDayProposer(), this.settingsRepository,
                this.rhythmService);
    }

    private void givenOwnedPetAndRules() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(PET);
        when(this.ruleLoader.loadAll()).thenReturn(List.of(ELEVEN_WEEKS));
    }

    @Test
    void createsSettingsWithMealsInTimeOrder() {
        this.givenOwnedPetAndRules();
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.empty());
        RhythmDayResponse day = RhythmDayResponse.notAvailable(TODAY, 11);
        when(this.rhythmService.buildDay(PET, TODAY)).thenReturn(day);

        RhythmDayResponse result = this.service.update(OWNER_ID, PET_ID,
                new DaySettings(420, 1350, List.of(930, 435, 1110, 720), 60), TODAY);

        ArgumentCaptor<PetRhythmSettings> saved = ArgumentCaptor.forClass(PetRhythmSettings.class);
        verify(this.settingsRepository).save(saved.capture());
        assertThat(saved.getValue().getMealMinutes()).containsExactly(435, 720, 930, 1110);
        assertThat(saved.getValue().getCustomAwakeMinutes()).isEqualTo(60);
        assertThat(saved.getValue().getCreatedOn()).isEqualTo(TODAY);
        assertThat(saved.getValue().getBandNoteSeenFromWeeks()).isEqualTo(10);
        assertThat(result).isSameAs(day);
    }

    @Test
    void updatesTheExistingSettingsInsteadOfCreatingAnotherSet() {
        this.givenOwnedPetAndRules();
        PetRhythmSettings existing = new PetRhythmSettings(null, TODAY, 10);
        existing.setWakeMinute(400);
        existing.replaceMealMinutes(List.of(500));
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.of(existing));

        this.service.update(OWNER_ID, PET_ID, new DaySettings(420, 1350, List.of(435, 720, 930, 1110), null), TODAY);

        verify(this.settingsRepository).save(existing);
        assertThat(existing.getWakeMinute()).isEqualTo(420);
        assertThat(existing.getMealMinutes()).hasSize(4);
        assertThat(existing.getCustomAwakeMinutes()).isNull();
    }

    @Test
    void changingTheMealCountSpreadsTheMealsAndKeepsTheDay() {
        this.givenOwnedPetAndRules();
        PetRhythmSettings existing = new PetRhythmSettings(null, TODAY, 10);
        existing.setWakeMinute(420);
        existing.setBedMinute(1350);
        existing.replaceMealMinutes(List.of(435, 720, 1110));
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.of(existing));

        this.service.updateMealsPerDay(OWNER_ID, PET_ID, 5, TODAY);

        verify(this.settingsRepository).save(existing);
        assertThat(existing.getMealMinutes()).hasSize(5).isSorted();
        assertThat(existing.getWakeMinute()).isEqualTo(420);
        assertThat(existing.getBedMinute()).isEqualTo(1350);
    }

    @Test
    void changingTheMealCountWithoutSavedSettingsStartsFromTheUsualDay() {
        this.givenOwnedPetAndRules();
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.empty());

        this.service.updateMealsPerDay(OWNER_ID, PET_ID, 4, TODAY);

        ArgumentCaptor<PetRhythmSettings> saved = ArgumentCaptor.forClass(PetRhythmSettings.class);
        verify(this.settingsRepository).save(saved.capture());
        assertThat(saved.getValue().getMealMinutes()).containsExactly(435, 660, 885, 1110);
    }

    @Test
    void marksTheBandNoteSeenForTheCurrentBand() {
        this.givenOwnedPetAndRules();
        PetRhythmSettings existing = new PetRhythmSettings(null, TODAY.minusWeeks(3), 8);
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.of(existing));

        this.service.markBandNoteSeen(OWNER_ID, PET_ID, TODAY);

        verify(this.settingsRepository).save(existing);
        assertThat(existing.getBandNoteSeenFromWeeks()).isEqualTo(10);
    }

    @Test
    void recordsTheCheckinAnswerOnTheClientDate() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(PET);
        PetRhythmSettings existing = new PetRhythmSettings(null, TODAY.minusDays(15), 10);
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.of(existing));

        this.service.answerCheckin(OWNER_ID, PET_ID, TODAY);

        verify(this.settingsRepository).save(existing);
        assertThat(existing.getCheckinAnsweredOn()).isEqualTo(TODAY);
    }

    @Test
    void noticesDoNothingBeforeTheRhythmIsSetUp() {
        when(this.petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(PET);
        when(this.settingsRepository.findByPetId(PET_ID)).thenReturn(Optional.empty());

        this.service.answerCheckin(OWNER_ID, PET_ID, TODAY);

        verify(this.settingsRepository, never()).save(any());
    }

    @Test
    void savesNothingWhenTheSettingsAreInvalid() {
        this.givenOwnedPetAndRules();
        doThrow(new MealsOutOfRange()).when(this.settingsValidator).validate(any(), any());

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, new DaySettings(420, 1350, List.of(435), null), TODAY))
                .isInstanceOf(MealsOutOfRange.class);
        verify(this.settingsRepository, never()).save(any());
    }

    @Test
    void rejectsADateTheClientValidatorRefuses() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.update(OWNER_ID, PET_ID, new DaySettings(420, 1350, List.of(435), null), TODAY))
                .isInstanceOf(InvalidDate.class);
        verify(this.settingsRepository, never()).save(any());
    }
}
