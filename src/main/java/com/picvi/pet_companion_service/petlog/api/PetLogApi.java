package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.Wellbeing;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.petlog.repository.PetLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** What other modules may ask the pet log. Nothing here exposes the log tables themselves. */
@Component
@RequiredArgsConstructor
public class PetLogApi {
    private final PetLogRepository logRepository;

    /** A log as other modules see it. */
    public record LogSnapshot(UUID logId, LogType type, LogDetail detail, Integer stoolScore, Wellbeing wellbeing,
                              Set<LogFlag> flags, LocalDate localDate, int minuteOfDay, Integer weightGrams,
                              String note, String label, String dose) {
        static LogSnapshot of(PetLog log) {
            return new LogSnapshot(log.getId(), log.getType(), log.getDetail(), log.getStoolScore(), log.getWellbeing(),
                    Set.copyOf(log.getFlags()), log.getLocalDate(), log.getMinuteOfDay(), log.getWeightGrams(), log.getNote(), log.getLabel(),
                    log.getDose());
        }
    }

    /** A dose that answered a planned time of a course. */
    public record CourseDoseGiven(UUID logId, LocalDate localDate, int minuteOfDay, LocalDate plannedOn, int plannedMinute) {
    }

    /** How many doses a course has had and when the last one was given. */
    public record CourseDoseTotals(int given, LocalDate lastOn, int lastMinute) {
    }

    /** A weight measurement and the day it was taken. */
    public record WeightPoint(int grams, LocalDate localDate) {
    }

    /** Meals that were logged (and not deleted since) after the given moment. */
    @Transactional(readOnly = true)
    public long mealsLoggedSince(UUID petId, OffsetDateTime since) {
        return this.logRepository.countMealsCreatedSince(petId, since);
    }

    /** Logs on or after a day, oldest first. */
    @Transactional(readOnly = true)
    public List<LogSnapshot> logsSince(UUID petId, LocalDate date) {
        return this.logRepository.findAllByPetIdAndLocalDateGreaterThanEqual(petId, date).stream()
                .map(LogSnapshot::of)
                .sorted(Comparator.comparing(LogSnapshot::localDate).thenComparing(LogSnapshot::minuteOfDay))
                .toList();
    }

    /** Logs between two days, both included, oldest first. */
    @Transactional(readOnly = true)
    public List<LogSnapshot> logsBetween(UUID petId, LocalDate from, LocalDate to) {
        return this.logRepository.findAllByPetIdAndLocalDateBetweenOrderByLocalDateAscMinuteOfDayAsc(petId, from, to).stream()
                .map(LogSnapshot::of)
                .toList();
    }

    /** The most recent weight logged on or before the given day. */
    @Transactional(readOnly = true)
    public Optional<WeightPoint> lastWeightBefore(UUID petId, LocalDate date) {
        return this.lastTwoWeightsBefore(petId, date).stream().findFirst();
    }

    /** Minutes since the last log of the given type, or empty if none. */
    @Transactional(readOnly = true)
    public OptionalLong minutesSinceLast(UUID petId, LogType type, LocalDate date, int minuteOfDay) {
        return this.logRepository.findAtOrBefore(petId, type, date, minuteOfDay, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(log -> ChronoUnit.DAYS.between(log.getLocalDate(), date) * 1440L
                        + minuteOfDay - log.getMinuteOfDay())
                .map(OptionalLong::of)
                .orElseGet(OptionalLong::empty);
    }

    /** The last two weights logged on or before the given day, newest first. */
    @Transactional(readOnly = true)
    public List<WeightPoint> lastTwoWeightsBefore(UUID petId, LocalDate date) {
        return this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                        petId, LogType.WEIGHT, date, PageRequest.of(0, 2)).stream()
                .filter(log -> log.getWeightGrams() != null)
                .map(log -> new WeightPoint(log.getWeightGrams(), log.getLocalDate()))
                .toList();
    }

    /** The most recent medicine logged on or before the given day. */
    @Transactional(readOnly = true)
    public Optional<LogSnapshot> lastMedicineBefore(UUID petId, LocalDate date) {
        return this.logRepository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(
                        petId, LogType.MEDICINE, date, PageRequest.of(0, 1)).stream()
                .findFirst()
                .map(LogSnapshot::of);
    }

    /** Logs between two days, both included, as the app sees them, newest first. */
    @Transactional(readOnly = true)
    public List<PetLogDto> logDtosBetween(UUID petId, LocalDate from, LocalDate to) {
        return this.logRepository.findAllByPetIdAndLocalDateBetweenOrderByLocalDateAscMinuteOfDayAsc(petId, from, to).stream()
                .map(PetLogDto::of)
                .sorted(Comparator.comparing(PetLogDto::localDate).thenComparing(PetLogDto::minuteOfDay).reversed())
                .toList();
    }

    /** Doses that answered a planned time between two days, both included, grouped by course. */
    @Transactional(readOnly = true)
    public Map<UUID, List<CourseDoseGiven>> courseDosesBetween(Collection<UUID> courseIds, LocalDate from, LocalDate to) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        return this.logRepository.findAllByCourseIdInAndDoseOnBetween(courseIds, from, to).stream()
                .collect(Collectors.groupingBy(PetLog::getCourseId, Collectors.mapping(
                        log -> new CourseDoseGiven(log.getId(), log.getLocalDate(), log.getMinuteOfDay(), log.getDoseOn(), log.getDoseMinute()),
                        Collectors.toList())));
    }

    /** Every dose of each course counted, with the newest one's time. Courses without a dose are absent. */
    @Transactional(readOnly = true)
    public Map<UUID, CourseDoseTotals> courseDoseTotals(Collection<UUID> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> counts = this.logRepository.countDosesByCourse(courseIds).stream()
                .collect(Collectors.toMap(PetLogRepository.CourseDoseCount::getCourseId, PetLogRepository.CourseDoseCount::getGiven));
        return this.logRepository.findLastDosePerCourse(courseIds).stream()
                .collect(Collectors.toMap(PetLog::getCourseId,
                        last -> new CourseDoseTotals(counts.get(last.getCourseId()).intValue(), last.getLocalDate(), last.getMinuteOfDay())));
    }

    /** The last meals logged, newest first. */
    @Transactional(readOnly = true)
    public List<LogSnapshot> lastMeals(UUID petId, int count) {
        return this.logRepository.findByPetIdAndTypeOrderByLocalDateDescMinuteOfDayDesc(petId, LogType.MEAL, PageRequest.of(0, count)).stream()
                .map(LogSnapshot::of)
                .toList();
    }
}
