package com.picvi.pet_companion_service.health.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.pet.PetNotFound;
import com.picvi.pet_companion_service.health.domain.DiaryFilter;
import com.picvi.pet_companion_service.health.domain.HealthSummaryRule;
import com.picvi.pet_companion_service.health.domain.WatchEvents;
import com.picvi.pet_companion_service.health.dto.HealthSummaryResponse;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.WeightPoint;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.triage.api.TriageApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthSummaryServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate ADDED_ON = TODAY.minusDays(100);

    @Mock
    private PetService petService;
    @Mock
    private PetLogApi petLogApi;
    @Mock
    private NutritionApi nutritionApi;
    @Mock
    private TriageApi triageApi;

    private HealthSummaryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("Europe/Warsaw"));
        this.service = new HealthSummaryService(new ClientDateValidator(clock), this.petService, this.petLogApi, this.nutritionApi,
                new HealthSummaryRule(new WatchEvents()), new DiaryFilter(), this.triageApi);
        lenient().when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(
                new PetProfile(PET, "Mila", null, null, ADDED_ON, null, null, 0));
        lenient().when(this.petLogApi.logsBetween(any(), any(), any())).thenReturn(List.of());
        lenient().when(this.petLogApi.lastTwoWeightsBefore(any(), any())).thenReturn(List.of());
        lenient().when(this.petLogApi.lastMedicineBefore(any(), any())).thenReturn(Optional.empty());
        lenient().when(this.petLogApi.logDtosBetween(any(), any(), any())).thenReturn(List.of());
        lenient().when(this.nutritionApi.changeNow(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(Optional.empty());
        lenient().when(this.triageApi.checksBetween(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void readsTheLastFourteenDaysOfLogsForTheCard() {
        this.service.summarise(OWNER, PET, TODAY);

        verify(this.petLogApi).logsBetween(PET, TODAY.minusDays(13), TODAY);
        verify(this.nutritionApi).changeNow(PET, TODAY, 14);
    }

    @Test
    void anEmptyWeekOfAnOldPetIsQuiet() {
        HealthSummaryResponse response = this.service.summarise(OWNER, PET, TODAY);

        assertThat(response.date()).isEqualTo(TODAY);
        assertThat(response.state().name()).isEqualTo("QUIET");
        assertThat(response.weight()).isNull();
        assertThat(response.lastMedicine()).isNull();
        assertThat(response.diary()).isEmpty();
    }

    @Test
    void aPetAddedYesterdayWithNoLogsIsSettlingIn() {
        when(this.petService.getOwnedPetProfile(OWNER, PET)).thenReturn(
                new PetProfile(PET, "Mila", null, null, TODAY.minusDays(1), null, null, 0));

        assertThat(this.service.summarise(OWNER, PET, TODAY).state().name()).isEqualTo("SETTLING_IN");
    }

    @Test
    void weightComesWithThePreviousOne() {
        when(this.petLogApi.lastTwoWeightsBefore(PET, TODAY)).thenReturn(
                List.of(new WeightPoint(12400, TODAY.minusDays(2)), new WeightPoint(12100, TODAY.minusDays(30))));

        HealthSummaryResponse.WeightDto weight = this.service.summarise(OWNER, PET, TODAY).weight();

        assertThat(weight).isEqualTo(new HealthSummaryResponse.WeightDto(12400, TODAY.minusDays(2), 12100, TODAY.minusDays(30)));
    }

    @Test
    void aSingleWeightHasNoPreviousOne() {
        when(this.petLogApi.lastTwoWeightsBefore(PET, TODAY)).thenReturn(List.of(new WeightPoint(12400, TODAY)));

        HealthSummaryResponse.WeightDto weight = this.service.summarise(OWNER, PET, TODAY).weight();

        assertThat(weight.previousGrams()).isNull();
        assertThat(weight.previousOn()).isNull();
    }

    @Test
    void lastMedicineIsReturned() {
        when(this.petLogApi.lastMedicineBefore(PET, TODAY)).thenReturn(Optional.of(new LogSnapshot(UUID.randomUUID(), LogType.MEDICINE,
                null, null, null, Set.of(), TODAY.minusDays(3), 480, null, null, "Bravecto", "1 tab")));

        assertThat(this.service.summarise(OWNER, PET, TODAY).lastMedicine())
                .isEqualTo(new HealthSummaryResponse.LastMedicineDto("Bravecto", "1 tab", TODAY.minusDays(3)));
    }

    @Test
    void diaryKeepsOnlyWhatMattersAndTheirOrder() {
        PetLogDto vomit = log(LogType.SYMPTOM, LogDetail.VOMITING, TODAY);
        PetLogDto walk = log(LogType.WALK, null, TODAY);
        PetLogDto refused = log(LogType.MEAL, LogDetail.REFUSED, TODAY.minusDays(1));
        PetLogDto ateAll = log(LogType.MEAL, LogDetail.ATE_ALL, TODAY.minusDays(1));
        when(this.petLogApi.logDtosBetween(PET, TODAY.minusDays(6), TODAY)).thenReturn(List.of(vomit, walk, refused, ateAll));

        assertThat(this.service.summarise(OWNER, PET, TODAY).diary()).containsExactly(vomit, refused);
    }

    @Test
    void aPetOfSomeoneElseIsNotFound() {
        when(this.petService.getOwnedPetProfile(OWNER, PET)).thenThrow(new PetNotFound());

        assertThatThrownBy(() -> this.service.summarise(OWNER, PET, TODAY)).isInstanceOf(PetNotFound.class);
        verifyNoInteractions(this.petLogApi, this.nutritionApi);
    }

    @Test
    void aDateFarFromTodayIsRejected() {
        assertThatThrownBy(() -> this.service.summarise(OWNER, PET, TODAY.plusDays(5))).isInstanceOf(InvalidDate.class);
        verifyNoInteractions(this.petService);
    }

    private static PetLogDto log(LogType type, LogDetail detail, LocalDate on) {
        return new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), type, detail, null, on, 600, on.atStartOfDay().atOffset(ZoneOffset.UTC), 0, null, null, List.of(), null, null, null, null, null, null,
                OWNER, Instant.parse("2026-10-15T08:00:00Z"), null);
    }
}
