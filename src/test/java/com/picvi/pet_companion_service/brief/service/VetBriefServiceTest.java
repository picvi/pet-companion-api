package com.picvi.pet_companion_service.brief.service;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import com.picvi.pet_companion_service.brief.domain.BriefReason;
import com.picvi.pet_companion_service.brief.domain.Concern;
import com.picvi.pet_companion_service.brief.domain.ConcernCounter;
import com.picvi.pet_companion_service.brief.domain.QuestionSuggester;
import com.picvi.pet_companion_service.brief.domain.Trend;
import com.picvi.pet_companion_service.brief.domain.TrendCalculator;
import com.picvi.pet_companion_service.brief.dto.BriefOptionsResponse;
import com.picvi.pet_companion_service.brief.dto.VetBriefResponse;
import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.care.dto.CareScheduleResponse;
import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.condition.api.ConditionApi;
import com.picvi.pet_companion_service.condition.api.ConditionApi.ConditionSnapshot;
import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionKind;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.brief.InvalidBriefRequest;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.medical.api.MedicalApi;
import com.picvi.pet_companion_service.medical.api.MedicalApi.VisitSnapshot;
import com.picvi.pet_companion_service.medication.api.MedicationApi;
import com.picvi.pet_companion_service.medication.api.MedicationApi.CourseInPeriod;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.dto.DietHistoryResponse;
import com.picvi.pet_companion_service.pet.dto.PetResponse;
import com.picvi.pet_companion_service.pet.entity.Sex;
import com.picvi.pet_companion_service.pet.entity.VaccinationStatus;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.WeightPoint;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VetBriefServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate BORN = LocalDate.of(2026, 7, 30);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private PetLogApi petLogApi;
    @Mock
    private ConditionApi conditionApi;
    @Mock
    private CareApi careApi;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private MedicalApi medicalApi;
    @Mock
    private MedicationApi medicationApi;

    private VetBriefService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T09:14:03Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new VetBriefService(this.clientDateValidator, this.petService, this.petLogApi, this.conditionApi, this.careApi,
                this.nutritionApi, this.medicalApi, this.medicationApi, new ConcernCounter(), new TrendCalculator(), new QuestionSuggester(), clock);
    }

    private void ownedPet() {
        when(this.petService.getPetById(OWNER, PET)).thenReturn(new PetResponse(PET, "Mila", Sex.FEMALE, BORN, false,
                "616093900123456", OWNER, null, "POODLE_MINIATURE", VaccinationStatus.UNKNOWN, SizeCategory.SMALL));
    }

    private void emptyBriefSources() {
        when(this.petLogApi.logsBetween(any(), any(), any())).thenReturn(List.of());
        when(this.petLogApi.lastWeightBefore(any(), any())).thenReturn(Optional.empty());
        when(this.conditionApi.openConditions(any(), any())).thenReturn(List.of());
        when(this.careApi.schedule(any(), any(), any())).thenReturn(new CareScheduleResponse(TODAY, 11, null, List.of()));
        when(this.nutritionApi.dietHistory(any(), any(), any())).thenAnswer(call ->
                new DietHistoryResponse(call.getArgument(1), call.getArgument(2), List.of(), List.of(), List.of(), List.of()));
        when(this.medicalApi.lastVisit(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void optionsWithNoLogsHaveNoConcernsAndASevenDayPeriod() {
        this.ownedPet();
        when(this.petLogApi.logsBetween(any(), any(), any())).thenReturn(List.of());

        BriefOptionsResponse options = this.service.options(OWNER, PET, TODAY);

        assertThat(options.concerns()).isEmpty();
        assertThat(options.defaultPeriod().from()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(options.defaultPeriod().to()).isEqualTo(TODAY);
    }

    @Test
    void optionsDefaultFromIsThreeDaysBeforeFirstSeenAndSortedByCount() {
        this.ownedPet();
        when(this.petLogApi.logsBetween(any(), any(), any())).thenReturn(List.of(
                loose(LocalDate.of(2026, 10, 12)), loose(LocalDate.of(2026, 10, 13)), loose(LocalDate.of(2026, 10, 14)),
                vomiting(LocalDate.of(2026, 10, 13))));

        BriefOptionsResponse options = this.service.options(OWNER, PET, TODAY);

        assertThat(options.concerns()).extracting(BriefOptionsResponse.ConcernOptionDto::code)
                .containsExactly(Concern.DIARRHOEA, Concern.VOMITING);
        assertThat(options.concerns().get(0).count()).isEqualTo(3);
        assertThat(options.concerns().get(0).defaultFrom()).isEqualTo(LocalDate.of(2026, 10, 9));
    }

    @Test
    void optionsDefaultFromIsNeverBeforeTheBirthDate() {
        this.ownedPet();
        LocalDate firstSeen = BORN.plusDays(1);
        when(this.petLogApi.logsBetween(any(), any(), any())).thenReturn(List.of(loose(firstSeen)));

        BriefOptionsResponse options = this.service.options(OWNER, PET, firstSeen.plusDays(2));

        assertThat(options.concerns().get(0).defaultFrom()).isEqualTo(BORN);
    }

    @Test
    void adHocBriefWithNoConcernAndNoLogsStillWorks() {
        this.ownedPet();
        this.emptyBriefSources();

        VetBriefResponse brief = this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, null, null);

        assertThat(brief.mainWorry()).isNull();
        assertThat(brief.days()).isEmpty();
        assertThat(brief.reason()).isEqualTo(BriefReason.CHECK_UP);
        assertThat(brief.period().from()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(brief.pet().ageWeeks()).isEqualTo(11);
        assertThat(brief.pet().microchipNumber()).isEqualTo("616093900123456");
        assertThat(brief.suggestedQuestions()).extracting(VetBriefResponse.QuestionDto::code).startsWith("GENERAL_HEALTH_CHECK");
        assertThat(brief.generatedAt()).isEqualTo(Instant.parse("2026-10-15T09:14:03Z"));
    }

    @Test
    void briefWithAConcernButNoLogsInThePeriodHasNoMainWorry() {
        this.ownedPet();
        this.emptyBriefSources();

        VetBriefResponse brief = this.service.brief(OWNER, PET, TODAY, "SOMETHING_WRONG", "DIARRHOEA", null, null);

        assertThat(brief.mainWorry()).isNull();
    }

    @Test
    void briefBuildsTheMainWorryDaysAndFlags() {
        this.ownedPet();
        this.emptyBriefSources();
        LocalDate d12 = LocalDate.of(2026, 10, 12);
        LocalDate d14 = LocalDate.of(2026, 10, 14);
        LogSnapshot mucus = new LogSnapshot(UUID.randomUUID(), LogType.TOILET, LogDetail.POO, 6, null, Set.of(LogFlag.MUCUS), d14, 505,
                null, null, null, null);
        when(this.petLogApi.logsBetween(any(), any(), any())).thenReturn(List.of(loose(d12), mucus, vomiting(d14),
                meal(LogDetail.ATE_ALL, d14), meal(LogDetail.REFUSED, d14)));

        VetBriefResponse brief = this.service.brief(OWNER, PET, TODAY, "SOMETHING_WRONG", "DIARRHOEA", null, null);

        assertThat(brief.mainWorry().concern()).isEqualTo(Concern.DIARRHOEA);
        assertThat(brief.mainWorry().count()).isEqualTo(2);
        assertThat(brief.mainWorry().trend()).isEqualTo(Trend.STEADY);
        assertThat(brief.mainWorry().daily()).hasSize(7);
        assertThat(brief.mainWorry().flags()).containsExactly(LogFlag.MUCUS);
        assertThat(brief.mainWorry().also()).extracting(VetBriefResponse.AlsoDto::concern)
                .containsExactlyInAnyOrder(Concern.VOMITING, Concern.LOW_APPETITE);
        assertThat(brief.days()).extracting(VetBriefResponse.DayDto::localDate).containsExactly(d14, d12);
        assertThat(brief.days().get(0).mealsLogged()).isEqualTo(2);
        assertThat(brief.days().get(0).events()).extracting(VetBriefResponse.EventDto::detail)
                .containsExactly(LogDetail.POO, LogDetail.VOMITING, LogDetail.REFUSED);
        assertThat(brief.food().mealsLogged()).isEqualTo(2);
    }

    @Test
    void briefRespectsFromAndTo() {
        this.ownedPet();
        this.emptyBriefSources();

        VetBriefResponse brief = this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertThat(brief.period().from()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(brief.period().to()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void fromAfterToIsRejected() {
        this.ownedPet();

        assertThatThrownBy(() -> this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, TODAY, TODAY.minusDays(1)))
                .isInstanceOf(InvalidBriefRequest.class)
                .hasMessage("from: INVALID");
    }

    @Test
    void aFourHundredDayPeriodIsRejected() {
        this.ownedPet();

        assertThatThrownBy(() -> this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, TODAY.minusDays(400), TODAY))
                .isInstanceOf(InvalidBriefRequest.class)
                .hasMessage("from: OUT_OF_RANGE");
    }

    @Test
    void missingReasonAndUnknownConcernAreRejected() {
        assertThatThrownBy(() -> this.service.brief(OWNER, PET, TODAY, null, null, null, null))
                .isInstanceOf(InvalidBriefRequest.class).hasMessage("reason: REQUIRED");
        assertThatThrownBy(() -> this.service.brief(OWNER, PET, TODAY, "CHECK_UP", "SNEEZING_FITS", null, null))
                .isInstanceOf(InvalidBriefRequest.class).hasMessage("concern: INVALID");
    }

    @Test
    void aPetOfAnotherOwnerIsNotFound() {
        when(this.petService.getPetById(OWNER, PET)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, null, null))
                .isInstanceOf(PetNotFound.class);
        assertThatThrownBy(() -> this.service.options(OWNER, PET, TODAY)).isInstanceOf(PetNotFound.class);
    }

    @Test
    void aClientDateOutOfRangeIsRejected() {
        doThrow(new InvalidDate()).when(this.clientDateValidator).validate(TODAY);

        assertThatThrownBy(() -> this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, null, null)).isInstanceOf(InvalidDate.class);
    }

    @Test
    void briefCarriesTheLastVisitOpenConditionsAndWeights() {
        this.ownedPet();
        this.emptyBriefSources();
        VisitSnapshot visit = new VisitSnapshot(UUID.randomUUID(), LocalDate.of(2026, 9, 28), List.of(), "VetCare");
        when(this.medicalApi.lastVisit(PET, TODAY)).thenReturn(Optional.of(visit));
        when(this.conditionApi.openConditions(PET, TODAY)).thenReturn(List.of(new ConditionSnapshot(ConditionKind.ALLERGY,
                IngredientCode.CHICKEN, ConditionCertainty.SUSPECTED, null, LocalDate.of(2026, 9, 20))));
        when(this.petLogApi.lastWeightBefore(PET, TODAY)).thenReturn(Optional.of(new WeightPoint(3400, LocalDate.of(2026, 10, 12))));
        when(this.petLogApi.lastWeightBefore(PET, LocalDate.of(2026, 10, 11))).thenReturn(Optional.of(new WeightPoint(3050, LocalDate.of(2026, 10, 5))));

        VetBriefResponse brief = this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, null, null);

        assertThat(brief.lastVisit()).isEqualTo(visit);
        assertThat(brief.conditions()).hasSize(1);
        assertThat(brief.conditions().get(0).certainty()).isEqualTo(ConditionCertainty.SUSPECTED);
        assertThat(brief.pet().weight().grams()).isEqualTo(3400);
        assertThat(brief.pet().weight().previousGrams()).isEqualTo(3050);
    }

    @Test
    void briefListsTheCoursesOfThePeriodWithTheirDatesAndDosesGiven() {
        this.ownedPet();
        this.emptyBriefSources();
        CourseInPeriod course = new CourseInPeriod("Metronidazole", 50, DoseUnit.TABLET, DoseSchedule.DAILY, CourseStatus.ACTIVE,
                LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19), 2);
        when(this.medicationApi.coursesBetween(PET, LocalDate.of(2026, 10, 9), TODAY, TODAY)).thenReturn(List.of(course));

        VetBriefResponse brief = this.service.brief(OWNER, PET, TODAY, "CHECK_UP", null, null, null);

        assertThat(brief.medicines()).containsExactly(course);
    }

    private static LogSnapshot loose(LocalDate day) {
        return new LogSnapshot(UUID.randomUUID(), LogType.TOILET, LogDetail.POO, 6, null, Set.of(), day, 505, null, null, null, null);
    }

    private static LogSnapshot vomiting(LocalDate day) {
        return new LogSnapshot(UUID.randomUUID(), LogType.SYMPTOM, LogDetail.VOMITING, null, null, Set.of(), day, 640, null, null, null, null);
    }

    private static LogSnapshot meal(LogDetail detail, LocalDate day) {
        return new LogSnapshot(UUID.randomUUID(), LogType.MEAL, detail, null, null, Set.of(), day, 720, null, null, null, null);
    }
}
