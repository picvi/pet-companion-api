package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.WeightPoint;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.petlog.repository.PetLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetLogApiTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Mock
    private PetLogRepository logRepository;

    private PetLogApi api;

    @BeforeEach
    void setUp() {
        this.api = new PetLogApi(this.logRepository);
    }

    @Test
    void lastTwoWeightsAreReturnedNewestFirst() {
        when(this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                eq(PET), eq(LogType.WEIGHT), eq(TODAY), any())).thenReturn(List.of(
                weight(2100, TODAY.minusDays(7)), weight(1800, TODAY.minusDays(14))));

        assertThat(this.api.lastTwoWeightsBefore(PET, TODAY)).containsExactly(
                new WeightPoint(2100, TODAY.minusDays(7)), new WeightPoint(1800, TODAY.minusDays(14)));
    }

    @Test
    void oneWeightGivesAListOfOne() {
        when(this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                eq(PET), eq(LogType.WEIGHT), eq(TODAY), any())).thenReturn(List.of(weight(2100, TODAY)));

        assertThat(this.api.lastTwoWeightsBefore(PET, TODAY)).containsExactly(new WeightPoint(2100, TODAY));
    }

    @Test
    void noWeightGivesNoLastWeight() {
        when(this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                eq(PET), eq(LogType.WEIGHT), eq(TODAY), any())).thenReturn(List.of());

        assertThat(this.api.lastWeightBefore(PET, TODAY)).isEmpty();
    }

    @Test
    void lastWeightIsTheNewest() {
        when(this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                eq(PET), eq(LogType.WEIGHT), eq(TODAY), any())).thenReturn(List.of(
                weight(2100, TODAY.minusDays(7)), weight(1800, TODAY.minusDays(14))));

        assertThat(this.api.lastWeightBefore(PET, TODAY)).contains(new WeightPoint(2100, TODAY.minusDays(7)));
    }

    @Test
    void lastMedicineIsTheNewestMedicineLog() {
        PetLog dewormer = medicine("Dewormer", "1 tablet", TODAY.minusDays(10));
        when(this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                eq(PET), eq(LogType.MEDICINE), eq(TODAY), any())).thenReturn(List.of(dewormer));

        Optional<LogSnapshot> last = this.api.lastMedicineBefore(PET, TODAY);

        assertThat(last).isPresent();
        assertThat(last.get().label()).isEqualTo("Dewormer");
        assertThat(last.get().dose()).isEqualTo("1 tablet");
        assertThat(last.get().localDate()).isEqualTo(TODAY.minusDays(10));
    }

    @Test
    void noMedicineGivesNothing() {
        when(this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                eq(PET), eq(LogType.MEDICINE), eq(TODAY), any())).thenReturn(List.of());

        assertThat(this.api.lastMedicineBefore(PET, TODAY)).isEmpty();
    }

    @Test
    void logDtosAreNewestFirst() {
        PetLog morning = log(LogType.NOTE, TODAY, 480);
        PetLog evening = log(LogType.NOTE, TODAY, 1080);
        PetLog yesterday = log(LogType.NOTE, TODAY.minusDays(1), 600);
        when(this.logRepository.findAllByPetIdAndLocalDateBetweenOrderByLocalDateAscMinuteOfDayAsc(PET, TODAY.minusDays(6), TODAY))
                .thenReturn(List.of(yesterday, morning, evening));

        List<PetLogDto> logs = this.api.logDtosBetween(PET, TODAY.minusDays(6), TODAY);

        assertThat(logs).extracting(PetLogDto::minuteOfDay).containsExactly(1080, 480, 600);
    }

    private static PetLog weight(int grams, LocalDate on) {
        return new PetLog(null, UUID.randomUUID(), LogType.WEIGHT, UUID.randomUUID(), null,
                new LogValues(null, null, null, null, grams, null, null, null, null, null), on, 600, loggedAt(on));
    }

    private static PetLog medicine(String label, String dose, LocalDate on) {
        return new PetLog(null, UUID.randomUUID(), LogType.MEDICINE, UUID.randomUUID(), null,
                new LogValues(null, null, null, null, null, null, null, label, dose, null), on, 600, loggedAt(on));
    }

    private static PetLog log(LogType type, LocalDate on, int minute) {
        return new PetLog(null, UUID.randomUUID(), type, UUID.randomUUID(), null, LogValues.none(), on,
                minute, loggedAt(on));
    }

    private static LoggedAt loggedAt(LocalDate on) {
        return new LoggedAt(OffsetDateTime.parse(on + "T10:00+02:00"), 120);
    }
}
