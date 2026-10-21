package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.petlog.dto.MedicineDto;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RecentLogsTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 14);

    private final RecentLogs recentLogs = new RecentLogs();

    @Test
    void mostRepeatedLogComesFirst() {
        List<PetLogDto> logs = List.of(
                log(LogType.MEAL, LogDetail.ATE_ALL, 40, null, null, 600),
                log(LogType.TOILET, LogDetail.PEE, null, null, null, 500),
                log(LogType.TOILET, LogDetail.PEE, null, null, null, 400),
                log(LogType.TOILET, LogDetail.PEE, null, null, null, 300));

        RecentLogsResult result = this.recentLogs.from(logs, null);

        assertThat(result.repeats()).hasSize(2);
        assertThat(result.repeats().get(0).detail()).isEqualTo(LogDetail.PEE);
    }

    @Test
    void keepsOnlyFourRepeats() {
        List<PetLogDto> logs = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            logs.add(log(LogType.MEAL, LogDetail.ATE_ALL, i * 10, null, null, 100 * i));
        }

        assertThat(this.recentLogs.from(logs, null).repeats()).hasSize(4);
    }

    @Test
    void ignoresTypesThatAreNotRepeatable() {
        List<PetLogDto> logs = List.of(log(LogType.WEIGHT, null, null, null, null, 300));

        assertThat(this.recentLogs.from(logs, null).repeats()).isEmpty();
    }

    @Test
    void foodsAreDistinctAndNewestFirst() {
        List<PetLogDto> logs = List.of(
                log(LogType.MEAL, null, null, "A", null, 300),
                log(LogType.MEAL, null, null, "B", null, 500),
                log(LogType.MEAL, null, null, "A", null, 100));

        assertThat(this.recentLogs.from(logs, null).foods()).containsExactly("B", "A");
    }

    @Test
    void medicineDoseComesFromTheNewestLog() {
        List<PetLogDto> logs = List.of(
                log(LogType.MEDICINE, null, null, "Enteroferment", "1 sachet", 300),
                log(LogType.MEDICINE, null, null, "Enteroferment", "2 sachets", 500));

        assertThat(this.recentLogs.from(logs, null).medicines()).containsExactly(new MedicineDto("Enteroferment", "2 sachets"));
    }

    @Test
    void lastWeightIsCopiedFromTheWeightLog() {
        PetLogDto weight = log(LogType.WEIGHT, null, null, null, null, 300);

        assertThat(this.recentLogs.from(List.of(), weight).lastWeight().localDate()).isEqualTo(DAY);
    }

    private static PetLogDto log(LogType type, LogDetail detail, Integer amountGrams, String label, String dose, int minute) {
        OffsetDateTime occurredAt = DAY.atStartOfDay().plusMinutes(minute).atOffset(java.time.ZoneOffset.ofHours(2));
        return new PetLogDto(UUID.randomUUID(), UUID.randomUUID(), type, detail, amountGrams, DAY, minute, occurredAt, 120, null, null, List.of(), type == LogType.WEIGHT ? 2300 : null, null, null, label, dose, null,
                UUID.randomUUID(), null, null);
    }

    @Test
    void petFoodsInRecentMealsComeNewestFirstWithoutRepeats() {
        UUID kibble = UUID.randomUUID();
        UUID wet = UUID.randomUUID();
        List<PetLogDto> logs = List.of(
                withFoods(log(LogType.MEAL, null, null, null, null, 300), kibble),
                withFoods(log(LogType.MEAL, null, null, null, null, 500), wet, kibble),
                log(LogType.MEAL, null, null, "Old label", null, 100));

        RecentLogsResult result = this.recentLogs.from(logs, null);

        assertThat(result.petFoodIds()).containsExactlyInAnyOrder(wet, kibble);
        assertThat(result.foods()).containsExactly("Old label");
    }

    private static PetLogDto withFoods(PetLogDto log, UUID... foodIds) {
        return new PetLogDto(log.id(), log.clientId(), log.type(), log.detail(), log.amountGrams(), log.localDate(), log.minuteOfDay(),
                log.occurredAt(), log.utcOffsetMinutes(), log.accident(), log.stoolScore(), log.flags(), log.weightGrams(),
                log.durationMinutes(), log.wellbeing(), log.label(), log.dose(), log.note(), log.loggedBy(), log.createdAt(),
                log.updatedAt(), java.util.Arrays.stream(foodIds).map(id -> new LogFood(id, null)).toList());
    }
}
