package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.petlog.dto.LastWeightDto;
import com.picvi.pet_companion_service.petlog.dto.MedicineDto;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.dto.RepeatDto;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Collectors;

public class RecentLogs {
    private static final Set<LogType> REPEATABLE_TYPES = Set.of(LogType.TOILET, LogType.MEAL, LogType.WALK, LogType.MEDICINE);
    private static final int MAX_REPEATS = 4;
    private static final int MAX_SUGGESTIONS = 5;

    public RecentLogsResult from(List<PetLogDto> lastFourteenDays, PetLogDto lastWeight) {
        List<PetLogDto> newestFirst = lastFourteenDays.stream()
                .sorted(Comparator.comparing(RecentLogs::instantOf).reversed())
                .toList();
        return new RecentLogsResult(this.repeats(newestFirst), this.foods(newestFirst), this.petFoodIds(newestFirst), this.medicines(newestFirst),
                lastWeight == null ? null : new LastWeightDto(lastWeight.weightGrams(), lastWeight.localDate()));
    }

    private List<RepeatDto> repeats(List<PetLogDto> newestFirst) {
        Map<RepeatDto, List<PetLogDto>> groups = newestFirst.stream()
                .filter(log -> REPEATABLE_TYPES.contains(log.type()))
                .collect(Collectors.groupingBy(RecentLogs::repeatOf, LinkedHashMap::new, Collectors.toList()));
        return groups.entrySet().stream()
                .sorted(Comparator.<Map.Entry<RepeatDto, List<PetLogDto>>>comparingInt(entry -> entry.getValue().size()).reversed())
                .limit(MAX_REPEATS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> foods(List<PetLogDto> newestFirst) {
        return newestFirst.stream()
                .filter(log -> log.type() == LogType.MEAL && log.label() != null)
                .map(PetLogDto::label)
                .distinct()
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    /** The pet's own foods in recent meals, newest first. The old free-text labels stay in {@code foods} for meals logged before. */
    private List<UUID> petFoodIds(List<PetLogDto> newestFirst) {
        return newestFirst.stream()
                .filter(log -> log.type() == LogType.MEAL)
                .flatMap(log -> log.foods().stream())
                .map(LogFood::petFoodId)
                .distinct()
                .limit(MAX_SUGGESTIONS)
                .toList();
    }

    private List<MedicineDto> medicines(List<PetLogDto> newestFirst) {
        Map<String, MedicineDto> newestPerLabel = new LinkedHashMap<>();
        for (PetLogDto log : newestFirst) {
            if (log.type() == LogType.MEDICINE && log.label() != null) {
                newestPerLabel.putIfAbsent(log.label(), new MedicineDto(log.label(), log.dose()));
            }
        }
        return newestPerLabel.values().stream().limit(MAX_SUGGESTIONS).toList();
    }

    private static RepeatDto repeatOf(PetLogDto log) {
        return new RepeatDto(log.type(), log.detail(), log.accident(), log.amountGrams(), log.label(), log.dose(),
                log.durationMinutes());
    }

    private static Instant instantOf(PetLogDto log) {
        return log.occurredAt().toInstant();
    }
}
