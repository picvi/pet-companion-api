package com.picvi.pet_companion_service.petlog.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.InvalidDate;
import com.picvi.pet_companion_service.exception.petlog.LogNotFound;
import com.picvi.pet_companion_service.exception.petlog.LogTimeInFuture;
import com.picvi.pet_companion_service.pet.dto.PetProfile;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogSaved;
import com.picvi.pet_companion_service.petlog.domain.LogChange;
import com.picvi.pet_companion_service.petlog.domain.LogCursor;
import com.picvi.pet_companion_service.exception.nutrition.FoodNotActive;
import com.picvi.pet_companion_service.nutrition.api.ActiveFood;
import com.picvi.pet_companion_service.nutrition.api.NutritionApi;
import com.picvi.pet_companion_service.petlog.domain.LogEdit;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogFood;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.RecentLogs;
import com.picvi.pet_companion_service.petlog.domain.RecentLogsResult;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.petlog.domain.OccurredAtCalculator;
import com.picvi.pet_companion_service.petlog.domain.PetLogRules;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.dto.PetLogHistoryResponse;
import com.picvi.pet_companion_service.petlog.dto.RecentLogsResponse;
import com.picvi.pet_companion_service.petlog.dto.PetLogsResponse;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.petlog.entity.PetLogChangeRow;
import com.picvi.pet_companion_service.petlog.repository.PetLogChangeRepository;
import com.picvi.pet_companion_service.petlog.repository.PetLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PetLogService {
    private static final int PAGE_SIZE = 50;

    private final ClientDateValidator clientDateValidator;
    private final PetService petService;
    private final PetLogRules logRules;
    private final PetLogRepository logRepository;
    private final PetLogChangeRepository changeRepository;
    private final OccurredAtCalculator occurredAtCalculator;
    private final Clock clock;
    private final RecentLogs recentLogs;
    private final NutritionApi nutritionApi;
    private final ApplicationEventPublisher events;

    public record LogResult(PetLogDto log, boolean created) {}

    public LogResult log(UUID ownerId, UUID petId, NewLog newLog, LocalDate today) {
        this.clientDateValidator.validate(today);
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);

        Optional<PetLog> existing = this.logRepository.findByPetIdAndClientId(petId, newLog.clientId());
        if (existing.isPresent()) {
            return new LogResult(PetLogDto.of(existing.get()), false);
        }

        this.logRules.validate(newLog.type(), newLog.detail(), newLog.values());
        this.validateLocalDate(newLog.localDate(), pet.birthDate(), today);
        this.validateFoods(petId, newLog.localDate(), newLog.values().foods());
        LoggedAt loggedAt = this.occurredAtCalculator.at(newLog.localDate(), newLog.minuteOfDay(), newLog.utcOffsetMinutes());

        LogValues values = this.withNewFoodFlag(petId, newLog.type(), newLog.localDate(), newLog.values());
        PetLog saved = this.logRepository.save(new PetLog(this.petService.getReference(petId), newLog.clientId(),
                newLog.type(), ownerId, newLog.detail(), values, newLog.localDate(),
                newLog.minuteOfDay(), loggedAt));
        this.recordChange(petId, saved, LogChange.CREATED);
        this.publishSaved(petId, saved);
        return new LogResult(PetLogDto.of(saved), true);
    }

    public PetLogDto update(UUID ownerId, UUID petId, UUID logId, LogEdit edit, LocalDate today) {
        this.clientDateValidator.validate(today);
        PetProfile pet = this.petService.getOwnedPetProfile(ownerId, petId);

        PetLog log = this.logRepository.findByIdAndPetId(logId, petId).orElseThrow(LogNotFound::new);
        this.logRules.validate(log.getType(), edit.detail(), edit.values());
        this.validateLocalDate(edit.localDate(), pet.birthDate(), today);
        this.validateFoods(petId, edit.localDate(), edit.values().foods());
        LoggedAt loggedAt = this.occurredAtCalculator.at(edit.localDate(), edit.minuteOfDay(), edit.utcOffsetMinutes());

        LogValues values = this.withNewFoodFlag(petId, log.getType(), edit.localDate(), edit.values());
        log.replace(edit.detail(), values, edit.localDate(), edit.minuteOfDay(), loggedAt,
                this.clock.instant());
        this.recordChange(petId, log, LogChange.UPDATED);
        this.publishSaved(petId, log);
        return PetLogDto.of(log);
    }

    @Transactional(readOnly = true)
    public PetLogsResponse list(UUID ownerId, UUID petId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);
        return new PetLogsResponse(this.logsOn(petId, date));
    }

    @Transactional(readOnly = true)
    public List<PetLogDto> logsOn(UUID petId, LocalDate date) {
        return this.logRepository.findAllByPetIdAndLocalDateOrderByMinuteOfDayAsc(petId, date).stream()
                .map(PetLogDto::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetLogHistoryResponse history(UUID ownerId, UUID petId, LocalDate today, Set<LogType> types, String before) {
        this.clientDateValidator.validate(today);
        this.petService.getOwnedPetProfile(ownerId, petId);

        int pageSize = PAGE_SIZE;
        Set<LogType> wanted = types == null || types.isEmpty() ? EnumSet.allOf(LogType.class) : types;
        LogCursor cursor = before == null || before.isBlank() ? LogCursor.start(today) : LogCursor.parse(before);

        List<PetLog> rows = this.logRepository.findPageBefore(petId, wanted, cursor.localDate(), cursor.minuteOfDay(),
                cursor.id(), PageRequest.of(0, pageSize + 1));
        boolean hasMore = rows.size() > pageSize;
        List<PetLog> page = hasMore ? rows.subList(0, pageSize) : rows;
        String nextCursor = hasMore ? this.cursorOf(page.get(page.size() - 1)).format() : null;
        return new PetLogHistoryResponse(page.stream().map(PetLogDto::of).toList(), nextCursor);
    }

    @Transactional(readOnly = true)
    public List<PetLogDto> timelinePage(UUID petId, Set<LogType> types, boolean flaggedToilet, LogCursor cursor, int limit) {
        PageRequest page = PageRequest.of(0, limit + 1);
        List<PetLog> rows = flaggedToilet
                ? this.logRepository.findPageBeforeIncludingFlaggedToilet(petId, types, cursor.localDate(), cursor.minuteOfDay(), cursor.id(), page)
                : this.logRepository.findPageBefore(petId, types, cursor.localDate(), cursor.minuteOfDay(), cursor.id(), page);
        return rows.stream().map(PetLogDto::of).toList();
    }

    @Transactional(readOnly = true)
    public RecentLogsResponse recent(UUID ownerId, UUID petId, LocalDate today) {
        this.clientDateValidator.validate(today);
        this.petService.getOwnedPetProfile(ownerId, petId);

        List<PetLogDto> lastFourteenDays = this.logRepository
                .findAllByPetIdAndLocalDateGreaterThanEqual(petId, today.minusDays(14)).stream()
                .map(PetLogDto::of)
                .toList();
        PetLogDto lastWeight = this.logRepository
                .findFirstByPetIdAndTypeOrderByLocalDateDescMinuteOfDayDesc(petId, LogType.WEIGHT)
                .map(PetLogDto::of)
                .orElse(null);
        RecentLogsResult result = this.recentLogs.from(lastFourteenDays, lastWeight);
        return new RecentLogsResponse(result.repeats(), result.foods(), result.petFoodIds(), result.medicines(), result.lastWeight());
    }

    public void undo(UUID ownerId, UUID petId, UUID logId, LocalDate date) {
        this.clientDateValidator.validate(date);
        this.petService.getOwnedPetProfile(ownerId, petId);

        PetLog log = this.logRepository.findByIdAndPetId(logId, petId).orElseThrow(LogNotFound::new);
        this.recordChange(petId, log, LogChange.DELETED);
        this.logRepository.delete(log);
    }

    private void publishSaved(UUID petId, PetLog log) {
        this.events.publishEvent(new PetLogSaved(petId, log.getId(), log.getType(), log.getDetail(), log.getStoolScore(),
                log.getWellbeing(), Set.copyOf(log.getFlags()), log.getLocalDate()));
    }

    private LogCursor cursorOf(PetLog log) {
        return new LogCursor(log.getLocalDate(), log.getMinuteOfDay(), log.getId());
    }

    private void recordChange(UUID petId, PetLog log, LogChange change) {
        this.changeRepository.save(new PetLogChangeRow(log.getId(), petId, change, OffsetDateTime.now(this.clock)));
    }

    /** A meal during a food change is a "new food" meal; the server sets the flag, the app does not send it. */
    private LogValues withNewFoodFlag(UUID petId, LogType type, LocalDate localDate, LogValues values) {
        if (type != LogType.MEAL || values.flags().contains(LogFlag.NEW_FOOD) || !this.nutritionApi.isFoodChangeOn(petId, localDate)) {
            return values;
        }
        return values.withFlag(LogFlag.NEW_FOOD);
    }

    /** Every food must be one of this pet's foods that was being given on the meal's day. */
    private void validateFoods(UUID petId, LocalDate localDate, List<LogFood> foods) {
        if (foods.isEmpty()) {
            return;
        }
        Set<UUID> active = this.nutritionApi.activeFoods(petId, localDate).stream()
                .map(ActiveFood::petFoodId)
                .collect(Collectors.toSet());
        if (!foods.stream().allMatch(food -> active.contains(food.petFoodId()))) {
            throw new FoodNotActive();
        }
    }

    private void validateLocalDate(LocalDate localDate, LocalDate birthDate, LocalDate today) {
        if (localDate.isAfter(today)) {
            throw new LogTimeInFuture();
        }
        if (localDate.isBefore(birthDate)) {
            throw new InvalidDate();
        }
    }
}
