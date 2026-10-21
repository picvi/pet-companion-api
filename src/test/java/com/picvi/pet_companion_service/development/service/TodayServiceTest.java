package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.ActivityCatalogueFixture;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.DevelopmentStage;
import com.picvi.pet_companion_service.development.domain.FocusReasonCode;
import com.picvi.pet_companion_service.development.domain.FocusType;
import com.picvi.pet_companion_service.development.domain.LifeStageCalculator;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.PlanReason;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;
import com.picvi.pet_companion_service.development.domain.StageProgressCalculator;
import com.picvi.pet_companion_service.development.domain.TodayPlanner;
import com.picvi.pet_companion_service.development.dto.PlanItemDto;
import com.picvi.pet_companion_service.development.dto.TodayResponse;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.domain.VaccinationPrompt;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodayServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 13);
    private static final LocalDate BIRTH_DATE_AT_TWELVE_WEEKS = TODAY.minusWeeks(12);
    private static final LocalDate ADDED_LONG_AGO = TODAY.minusWeeks(4);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private ActivityDataLoader activityDataLoader;

    private TodayService service;

    @BeforeEach
    void setUp() {
        TodayPlanner planner = new TodayPlanner(new LifeStageCalculator(), new StageProgressCalculator(), new VaccinationPrompt());
        service = new TodayService(clientDateValidator, petService, activityDataLoader, planner);
    }

    @Test
    void responseMapsStageFocusAndPlan() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, ADDED_LONG_AGO);
        givenCatalogue(Map.of());

        TodayResponse response = service.getToday(OWNER_ID, PET_ID, TODAY);

        verify(clientDateValidator).validate(TODAY);
        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.petName()).isEqualTo("Luna");
        assertThat(response.ageWeeks()).isEqualTo(12);
        assertThat(response.stage().code()).isEqualTo(DevelopmentStage.FINDING_CONFIDENCE);
        assertThat(response.stage().weeksLeft()).isEqualTo(4);
        assertThat(response.stage().weeksLeftInWindow()).isEqualTo(4);
        assertThat(response.stage().completed()).isZero();
        assertThat(response.stage().total()).isPositive();
        assertThat(response.focus().type()).isEqualTo(FocusType.ACTIVITY);
        assertThat(response.focus().activity().kind()).isEqualTo(ActivityKind.SOCIALIZATION);
        assertThat(response.focus().reason().code()).isEqualTo(FocusReasonCode.SOCIALISATION_WINDOW);
        assertThat(response.focus().doneToday()).isFalse();
        assertThat(response.plan()).extracting(PlanItemDto::reason).contains(PlanReason.RECURRING_DUE);
        assertThat(response.plan()).hasSizeLessThanOrEqualTo(3);
        assertThat(response.showWorriedCard()).isFalse();
        assertThat(response.vaccinationPromptDue()).isFalse();
    }

    @Test
    void focusWithoutActivityHasNoActivityReference() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, TODAY);
        givenCatalogue(Map.of());

        TodayResponse response = service.getToday(OWNER_ID, PET_ID, TODAY);

        assertThat(response.focus().type()).isEqualTo(FocusType.WELCOME);
        assertThat(response.focus().activity()).isNull();
        assertThat(response.plan()).extracting(PlanItemDto::reason).containsOnly(PlanReason.HOME_BASICS);
    }

    @Test
    void doneTodayReflectsRecordedProgress() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, ADDED_LONG_AGO);
        givenCatalogue(Map.of());
        String focusCode = service.getToday(OWNER_ID, PET_ID, TODAY).focus().activity().code();
        givenCatalogue(Map.of(focusCode,
                new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, TODAY, TODAY, TODAY, null)));

        TodayResponse response = service.getToday(OWNER_ID, PET_ID, TODAY);

        assertThat(response.focus().activity().code()).isEqualTo(focusCode);
        assertThat(response.focus().doneToday()).isTrue();
        assertThat(response.stage().completed()).isEqualTo(1);
    }

    @Test
    void someoneElsesPetIsNotFound() {
        when(petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> service.getToday(OWNER_ID, PET_ID, TODAY)).isInstanceOf(PetNotFound.class);
    }

    @Test
    void dateBeforeBirthIsRejected() {
        givenPet(TODAY.plusDays(1), TODAY);

        assertThatThrownBy(() -> service.getToday(OWNER_ID, PET_ID, TODAY)).isInstanceOf(InvalidDate.class);
    }

    private void givenPet(LocalDate birthDate, LocalDate createdOn) {
        when(petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(new PetProfile(
                PET_ID, "Luna", birthDate, SizeCategory.MEDIUM, createdOn, VaccinationStatus.NONE, null, 0));
    }

    private void givenCatalogue(Map<String, ProgressState> progress) {
        when(activityDataLoader.loadActiveActivities()).thenReturn(ActivityCatalogueFixture.load());
        when(activityDataLoader.loadProgress(PET_ID)).thenReturn(progress);
    }
}
