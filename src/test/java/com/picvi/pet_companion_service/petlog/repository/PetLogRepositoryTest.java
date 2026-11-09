package com.picvi.pet_companion_service.petlog.repository;

import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PetLogRepositoryTest extends PostgresTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 10);
    private static final UUID NO_CURSOR_ID = new UUID(-1, -1);

    @Autowired
    private PetLogRepository repository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID userId;
    private UUID petId;
    private Pet pet;

    @BeforeEach
    void setUp() {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        this.userId = seed.user();
        this.petId = seed.pet(this.userId);
        this.pet = this.petRepository.getReferenceById(this.petId);
    }

    @Test
    void pageBeforeReturnsNewestFirstAndOnlyTheRequestedTypes() {
        this.save(LogType.MEAL, DAY, 480);
        this.save(LogType.WEIGHT, DAY, 500);
        this.save(LogType.MEAL, DAY.minusDays(1), 900);
        this.save(LogType.MEAL, DAY.plusDays(1), 100);

        List<PetLog> page = this.repository.findPageBefore(this.petId, Set.of(LogType.MEAL), DAY.plusDays(1), 0, NO_CURSOR_ID, PageRequest.of(0, 10));

        assertThat(page).extracting(PetLog::getLocalDate, PetLog::getMinuteOfDay)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(DAY, 480), org.assertj.core.groups.Tuple.tuple(DAY.minusDays(1), 900));
    }

    @Test
    void pageBeforeStopsAtTheCursor() {
        PetLog first = this.save(LogType.MEAL, DAY, 480);
        this.save(LogType.MEAL, DAY, 300);

        List<PetLog> page = this.repository.findPageBefore(this.petId, Set.of(LogType.MEAL), DAY, 480, first.getId(), PageRequest.of(0, 10));

        assertThat(page).extracting(PetLog::getMinuteOfDay).containsExactly(300);
    }

    @Test
    void flaggedToiletIsIncludedEvenWhenToiletIsNotRequested() {
        this.save(LogType.TOILET, DAY, 400, new LogValues(null, 6, Set.of(LogFlag.BLOOD), null, null, null, null, null, null, null));
        this.save(LogType.TOILET, DAY, 410);
        this.save(LogType.MEAL, DAY, 420);

        List<PetLog> page = this.repository.findPageBeforeIncludingFlaggedToilet(this.petId, Set.of(LogType.MEAL), DAY.plusDays(1), 0, NO_CURSOR_ID, PageRequest.of(0, 10));

        assertThat(page).extracting(PetLog::getType, PetLog::getMinuteOfDay)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(LogType.MEAL, 420), org.assertj.core.groups.Tuple.tuple(LogType.TOILET, 400));
    }

    @Test
    void betweenReturnsOldestFirstInsideTheRange() {
        this.save(LogType.MEAL, DAY.plusDays(2), 100);
        this.save(LogType.MEAL, DAY, 700);
        this.save(LogType.MEAL, DAY, 200);
        this.save(LogType.MEAL, DAY.minusDays(5), 100);

        List<PetLog> logs = this.repository.findAllByPetIdAndLocalDateBetweenOrderByLocalDateAscMinuteOfDayAsc(this.petId, DAY, DAY.plusDays(2));

        assertThat(logs).extracting(PetLog::getLocalDate, PetLog::getMinuteOfDay)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(DAY, 200), org.assertj.core.groups.Tuple.tuple(DAY, 700), org.assertj.core.groups.Tuple.tuple(DAY.plusDays(2), 100));
    }

    @Test
    void lastOfTypeIsTheNewestOneOnOrBeforeTheDate() {
        this.save(LogType.WEIGHT, DAY.minusDays(3), 600, new LogValues(null, null, null, null, 4000, null, null, null, null, null));
        this.save(LogType.WEIGHT, DAY, 600, new LogValues(null, null, null, null, 4200, null, null, null, null, null));
        this.save(LogType.WEIGHT, DAY.plusDays(4), 600, new LogValues(null, null, null, null, 4500, null, null, null, null, null));

        List<PetLog> onOrBefore = this.repository.findByPetIdAndTypeAndLocalDateLessThanEqualOrderByLocalDateDescMinuteOfDayDesc(this.petId, LogType.WEIGHT, DAY, PageRequest.of(0, 1));
        PetLog newest = this.repository.findFirstByPetIdAndTypeOrderByLocalDateDescMinuteOfDayDesc(this.petId, LogType.WEIGHT).orElseThrow();

        assertThat(onOrBefore).extracting(PetLog::getWeightGrams).containsExactly(4200);
        assertThat(newest.getWeightGrams()).isEqualTo(4500);
    }

    @Test
    void mealsCreatedSinceCountsOnlyCreatedMealsAfterTheMoment() {
        PetLog meal = this.save(LogType.MEAL, DAY, 480);
        PetLog toilet = this.save(LogType.TOILET, DAY, 490);
        this.change(meal, "CREATED", "2026-10-10T09:00:00Z");
        this.change(toilet, "CREATED", "2026-10-10T09:05:00Z");
        this.change(meal, "UPDATED", "2026-10-10T09:10:00Z");

        long since = this.repository.countMealsCreatedSince(this.petId, OffsetDateTime.parse("2026-10-10T08:00:00Z"));
        long after = this.repository.countMealsCreatedSince(this.petId, OffsetDateTime.parse("2026-10-10T09:30:00Z"));

        assertThat(since).isEqualTo(1);
        assertThat(after).isZero();
    }

    @Test
    void flagsAreLoadedWithTheLog() {
        this.save(LogType.TOILET, DAY, 400, new LogValues(null, 6, Set.of(LogFlag.BLOOD, LogFlag.MUCUS), null, null, null, null, null, null, null));
        this.repository.flush();

        PetLog reloaded = this.repository.findAllByPetIdAndLocalDateOrderByMinuteOfDayAsc(this.petId, DAY).getFirst();

        assertThat(reloaded.getFlags()).containsExactlyInAnyOrder(LogFlag.BLOOD, LogFlag.MUCUS);
        assertThat(reloaded.getStoolScore()).isEqualTo(6);
    }

    private PetLog save(LogType type, LocalDate date, int minute) {
        return this.save(type, date, minute, LogValues.none());
    }

    private PetLog save(LogType type, LocalDate date, int minute, LogValues values) {
        LoggedAt loggedAt = new LoggedAt(OffsetDateTime.parse(date + "T10:00+02:00"), 120);
        return this.repository.saveAndFlush(new PetLog(this.pet, UUID.randomUUID(), type, this.userId, null, values, date, minute, loggedAt, null));
    }

    private void change(PetLog log, String change, String at) {
        this.jdbc.update("insert into pet_log_change (log_id, pet_id, change, changed_at) values (?, ?, ?, ?::timestamptz)", log.getId(), this.petId, change, at);
    }
}
