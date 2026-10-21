package com.picvi.pet_companion_service.development.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.domain.ActivityDisplayStatus;
import com.picvi.pet_companion_service.development.domain.ActivityGroup;
import com.picvi.pet_companion_service.development.domain.ActivityRanking;
import com.picvi.pet_companion_service.development.domain.ActivitySection;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.DevelopmentStage;
import com.picvi.pet_companion_service.development.domain.LifeStageCalculator;
import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;
import com.picvi.pet_companion_service.development.domain.StageProgressCalculator;
import com.picvi.pet_companion_service.development.domain.VaccinationRequirement;
import com.picvi.pet_companion_service.development.dto.DevelopmentActivityDto;
import com.picvi.pet_companion_service.development.dto.DevelopmentResponse;
import com.picvi.pet_companion_service.development.dto.StageWindowDto;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelopmentServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID PET_ID = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 10, 13);
    private static final LocalDate BIRTH_DATE_AT_TWELVE_WEEKS = DAY.minusWeeks(12);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private ActivityDataLoader activityDataLoader;

    private final Map<String, ProgressState> progressByCode = new HashMap<>();
    private DevelopmentService service;

    @BeforeEach
    void setUp() {
        service = new DevelopmentService(clientDateValidator, petService, activityDataLoader,
                new LifeStageCalculator(), new StageProgressCalculator(), new ActivityRanking());
        lenient().when(activityDataLoader.loadProgress(PET_ID)).thenReturn(progressByCode);
    }

    @Test
    void activitiesAreReturnedInRankOrder() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.UNKNOWN);
        ActivityDefinition done = activity("DONE", VaccinationRequirement.NONE);
        givenActivities(
                done,
                activity("OPEN_HARD", ActivityKind.SKILL, ActivityCategory.CARE, 3, 10),
                activity("OPEN_EASY_B", ActivityKind.SKILL, ActivityCategory.CARE, 1, 20),
                activity("OPEN_EASY_A", ActivityKind.SKILL, ActivityCategory.CARE, 1, 10));
        givenProgress(done, new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(response.activities()).extracting(DevelopmentActivityDto::code)
                .containsExactly("OPEN_EASY_A", "OPEN_EASY_B", "OPEN_HARD", "DONE");
        assertThat(response.activities()).extracting(DevelopmentActivityDto::rank).containsExactly(0, 1, 2, 3);
    }

    @Test
    void activityCarriesGroupAndSection() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.FIRST_DOSE);
        ActivityDefinition resting = activity("RESTING", VaccinationRequirement.NONE);
        givenActivities(resting, activity("LOCKED", VaccinationRequirement.SERIES_COMPLETE));
        givenProgress(resting, new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.SCARED, 1, DAY, DAY, null, null));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(dtoOf(response, "RESTING").group()).isEqualTo(ActivityGroup.MEETING_OTHERS);
        assertThat(dtoOf(response, "RESTING").section()).isEqualTo(ActivitySection.RESTING);
        assertThat(dtoOf(response, "RESTING").availableFrom()).isEqualTo(DAY.plusDays(7));
        assertThat(dtoOf(response, "LOCKED").section()).isEqualTo(ActivitySection.LOCKED);
        assertThat(dtoOf(response, "LOCKED").availableFrom()).isNull();
    }

    @Test
    void stagesUseTheSizeSpecificWindows() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.UNKNOWN, SizeCategory.SMALL);
        givenActivities();

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(response.stages()).extracting(StageWindowDto::code).containsExactly(DevelopmentStage.values());
        assertThat(windowOf(response, DevelopmentStage.ADOLESCENCE).endWeek()).isEqualTo(52);
        assertThat(windowOf(response, DevelopmentStage.YOUNG_ADULT).startWeek()).isEqualTo(52);
        assertThat(windowOf(response, DevelopmentStage.YOUNG_ADULT).endWeek()).isNull();
    }

    @Test
    void giantDogsHaveALongerAdolescence() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.UNKNOWN, SizeCategory.GIANT);
        givenActivities();

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(windowOf(response, DevelopmentStage.ADOLESCENCE).endWeek()).isEqualTo(104);
        assertThat(windowOf(response, DevelopmentStage.HOMECOMING).startWeek()).isZero();
    }

    @Test
    void activityWhoseVaccinationRequirementIsNotMetIsLocked() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.FIRST_DOSE);
        givenActivities(
                activity("OPEN", VaccinationRequirement.FIRST_DOSE),
                activity("LOCKED", VaccinationRequirement.SERIES_COMPLETE));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(statusOf(response, "OPEN")).isEqualTo(ActivityDisplayStatus.TODO);
        assertThat(statusOf(response, "LOCKED")).isEqualTo(ActivityDisplayStatus.LOCKED);
    }

    @Test
    void lockedWinsOverInProgressButNotOverCompleted() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.NONE);
        ActivityDefinition inProgress = activity("STARTED_AND_LOCKED", VaccinationRequirement.FIRST_DOSE);
        ActivityDefinition completed = activity("DONE_AND_LOCKED", VaccinationRequirement.FIRST_DOSE);
        givenActivities(inProgress, completed);
        givenProgress(inProgress, new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.UNSURE, 1, DAY, DAY, null, null));
        givenProgress(completed, new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(statusOf(response, "STARTED_AND_LOCKED")).isEqualTo(ActivityDisplayStatus.LOCKED);
        assertThat(statusOf(response, "DONE_AND_LOCKED")).isEqualTo(ActivityDisplayStatus.COMPLETED);
    }

    @Test
    void notPossibleActivityIsStillListedSoItCanBeRestored() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.UNKNOWN);
        ActivityDefinition hidden = activity("HIDDEN", VaccinationRequirement.NONE);
        givenActivities(hidden);
        givenProgress(hidden, new ProgressState(ProgressStatus.NOT_POSSIBLE, null, 0, null, null, null, null));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(statusOf(response, "HIDDEN")).isEqualTo(ActivityDisplayStatus.NOT_POSSIBLE);
    }

    @Test
    void onlyActivitiesOverlappingTheCurrentStageAreListed() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.UNKNOWN);
        givenActivities(
                new ActivityDefinition("CURRENT", ActivityKind.SKILL, ActivityCategory.CARE, 8, 20, 1,
                        VaccinationRequirement.NONE, null, 10),
                new ActivityDefinition("TOO_EARLY", ActivityKind.SKILL, ActivityCategory.CARE, 0, 10, 1,
                        VaccinationRequirement.NONE, null, 10),
                new ActivityDefinition("TOO_LATE", ActivityKind.SKILL, ActivityCategory.CARE, 16, 30, 1,
                        VaccinationRequirement.NONE, null, 10));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(response.activities()).extracting(DevelopmentActivityDto::code).containsExactly("CURRENT");
    }

    @Test
    void responseDescribesStageNextStageAndProgress() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.FIRST_DOSE);
        ActivityDefinition done = activity("DONE", VaccinationRequirement.NONE);
        givenActivities(done, activity("OPEN", VaccinationRequirement.NONE));
        givenProgress(done, new ProgressState(ProgressStatus.COMPLETED, Outcome.CALM, 1, DAY, DAY, DAY, null));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(response.date()).isEqualTo(DAY);
        assertThat(response.ageWeeks()).isEqualTo(12);
        assertThat(response.stage().code()).isEqualTo(DevelopmentStage.FINDING_CONFIDENCE);
        assertThat(response.stage().startWeek()).isEqualTo(10);
        assertThat(response.stage().endWeek()).isEqualTo(16);
        assertThat(response.stage().weeksLeft()).isEqualTo(4);
        assertThat(response.nextStage().code()).isEqualTo(DevelopmentStage.TEETHING);
        assertThat(response.nextStage().startsAtWeek()).isEqualTo(16);
        assertThat(response.progress().completed()).isEqualTo(1);
        assertThat(response.progress().total()).isEqualTo(2);
        assertThat(response.vaccinationStatus()).isEqualTo(VaccinationStatus.FIRST_DOSE);
    }

    @Test
    void youngAdultHasNoNextStage() {
        givenPet(DAY.minusWeeks(200), VaccinationStatus.UNKNOWN);
        givenActivities();

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(response.stage().code()).isEqualTo(DevelopmentStage.YOUNG_ADULT);
        assertThat(response.stage().endWeek()).isNull();
        assertThat(response.nextStage()).isNull();
    }

    @Test
    void recurringActivityIsDueNowOnlyWhenItsIntervalHasPassed() {
        givenPet(BIRTH_DATE_AT_TWELVE_WEEKS, VaccinationStatus.UNKNOWN);
        ActivityDefinition dueWeighIn = recurring("DUE", 7);
        ActivityDefinition recentWeighIn = recurring("RECENT", 7);
        givenActivities(dueWeighIn, recentWeighIn, activity("PLAIN", VaccinationRequirement.NONE));
        givenProgress(dueWeighIn, new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.PRACTISED, 1,
                DAY.minusDays(7), DAY.minusDays(7), null, null));
        givenProgress(recentWeighIn, new ProgressState(ProgressStatus.IN_PROGRESS, Outcome.PRACTISED, 1,
                DAY.minusDays(1), DAY.minusDays(1), null, null));

        DevelopmentResponse response = service.getDevelopment(OWNER_ID, PET_ID, DAY);

        assertThat(dtoOf(response, "DUE").recurring()).isTrue();
        assertThat(dtoOf(response, "DUE").dueNow()).isTrue();
        assertThat(dtoOf(response, "RECENT").dueNow()).isFalse();
        assertThat(dtoOf(response, "PLAIN").recurring()).isFalse();
        assertThat(dtoOf(response, "PLAIN").dueNow()).isFalse();
    }

    @Test
    void someoneElsesPetIsNotFound() {
        when(petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> service.getDevelopment(OWNER_ID, PET_ID, DAY)).isInstanceOf(PetNotFound.class);
    }

    @Test
    void dateBeforeBirthIsRejected() {
        givenPet(DAY.plusDays(1), VaccinationStatus.UNKNOWN);

        assertThatThrownBy(() -> service.getDevelopment(OWNER_ID, PET_ID, DAY)).isInstanceOf(InvalidDate.class);
    }

    private void givenPet(LocalDate birthDate, VaccinationStatus vaccinationStatus) {
        givenPet(birthDate, vaccinationStatus, SizeCategory.MEDIUM);
    }

    private void givenPet(LocalDate birthDate, VaccinationStatus vaccinationStatus, SizeCategory size) {
        when(petService.getOwnedPetProfile(OWNER_ID, PET_ID)).thenReturn(new PetProfile(
                PET_ID, "Luna", birthDate, size, birthDate, vaccinationStatus, null, 0));
    }

    private static StageWindowDto windowOf(DevelopmentResponse response, DevelopmentStage stage) {
        return response.stages().stream().filter(window -> window.code() == stage).findFirst().orElseThrow();
    }

    private void givenActivities(ActivityDefinition... definitions) {
        when(activityDataLoader.loadActiveActivities()).thenReturn(List.of(definitions));
    }

    private void givenProgress(ActivityDefinition definition, ProgressState state) {
        progressByCode.put(definition.code(), state);
    }

    private static ActivityDisplayStatus statusOf(DevelopmentResponse response, String code) {
        return dtoOf(response, code).status();
    }

    private static DevelopmentActivityDto dtoOf(DevelopmentResponse response, String code) {
        return response.activities().stream().filter(dto -> dto.code().equals(code)).findFirst().orElseThrow();
    }

    private static ActivityDefinition activity(String code, VaccinationRequirement requirement) {
        return new ActivityDefinition(code, ActivityKind.SOCIALIZATION, ActivityCategory.PEOPLE, 8, 20, 1,
                requirement, null, 10);
    }

    private static ActivityDefinition activity(String code, ActivityKind kind, ActivityCategory category,
                                               int difficulty, int sortOrder) {
        return new ActivityDefinition(code, kind, category, 8, 20, difficulty,
                VaccinationRequirement.NONE, null, sortOrder);
    }

    private static ActivityDefinition recurring(String code, int everyDays) {
        return new ActivityDefinition(code, ActivityKind.SKILL, ActivityCategory.CARE, 8, 52, 1,
                VaccinationRequirement.NONE, everyDays, 10);
    }
}
