package com.picvi.pet_companion_service.petlog.dto;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import com.picvi.pet_companion_service.petlog.entity.PetLog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PetLogDto(UUID id,
                        UUID clientId,
                        LogType type,
                        LogDetail detail,
                        Integer amountGrams,
                        LocalDate localDate,
                        int minuteOfDay,
                        OffsetDateTime occurredAt,
                        int utcOffsetMinutes,
                        Boolean accident,
                        Integer stoolScore,
                        List<LogFlag> flags,
                        Integer weightGrams,
                        Integer durationMinutes,
                        Wellbeing wellbeing,
                        String label,
                        String dose,
                        String note,
                        UUID loggedBy,
                        Instant createdAt,
                        Instant updatedAt,
                        List<LogFood> foods) {

    public PetLogDto(UUID id, UUID clientId, LogType type, LogDetail detail, Integer amountGrams, LocalDate localDate,
                     int minuteOfDay, OffsetDateTime occurredAt, int utcOffsetMinutes, Boolean accident,
                     Integer stoolScore, List<LogFlag> flags, Integer weightGrams, Integer durationMinutes,
                     Wellbeing wellbeing, String label, String dose, String note, UUID loggedBy, Instant createdAt,
                     Instant updatedAt) {
        this(id, clientId, type, detail, amountGrams, localDate, minuteOfDay, occurredAt, utcOffsetMinutes, accident,
                stoolScore, flags, weightGrams, durationMinutes, wellbeing, label, dose, note, loggedBy, createdAt, updatedAt,
                List.of());
    }

    public static PetLogDto of(PetLog log) {
        OffsetDateTime occurredAt = log.getOccurredAt()
                .withOffsetSameInstant(ZoneOffset.ofTotalSeconds(log.getUtcOffsetMinutes() * 60));
                List<LogFlag> flags = log.getFlags().stream().sorted(Comparator.naturalOrder()).toList();
        List<LogFood> foods = log.getFoods().stream()
                .map(food -> new LogFood(food.getPetFoodId(), food.getGrams()))
                .sorted(Comparator.comparing(LogFood::petFoodId))
                .toList();
        return new PetLogDto(log.getId(), log.getClientId(), log.getType(), log.getDetail(), log.getAmountGrams(),
                log.getLocalDate(), log.getMinuteOfDay(), occurredAt, log.getUtcOffsetMinutes(),
                log.getAccident(), log.getStoolScore(), flags, log.getWeightGrams(), log.getDurationMinutes(),
                log.getWellbeing(), log.getLabel(), log.getDose(), log.getNote(), log.getLoggedBy(),
                log.getCreatedAt(), log.getUpdatedAt(), foods);
    }
}
