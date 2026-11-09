package com.picvi.pet_companion_service.db;

import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationTest extends PostgresTest {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyMigrationAppliesOnAnEmptyDatabase() {
        Integer failed = this.jdbc.queryForObject("select count(*) from flyway_schema_history where success = false", Integer.class);

        assertThat(failed).isZero();
    }

    @Test
    void petLogTableExists() {
        Integer columns = this.jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_name = 'pet_log'", Integer.class);

        assertThat(columns).isPositive();
    }

    @Test
    void aDoseAnswersAPlannedTimeOnlyOnce() {
        UUID[] pet = ownerAndPet();
        UUID course = course(pet[1], "DAILY", null, null);
        doseLog(pet, course, LocalDate.of(2026, 10, 1), 480);

        assertThatThrownBy(() -> doseLog(pet, course, LocalDate.of(2026, 10, 1), 480))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void severalDosesWithoutAPlannedTimeAreAllowed() {
        UUID[] pet = ownerAndPet();
        UUID course = course(pet[1], "AS_NEEDED", null, 3);

        doseLog(pet, course, null, null);
        doseLog(pet, course, null, null);

        Integer logs = this.jdbc.queryForObject("select count(*) from pet_log where course_id = ?", Integer.class, course);
        assertThat(logs).isEqualTo(2);
    }

    @Test
    void aLogCannotPointAtAnotherPetsCourse() {
        UUID[] pet = ownerAndPet();
        UUID[] otherPet = ownerAndPet();
        UUID othersCourse = course(otherPet[1], "DAILY", null, null);

        assertThatThrownBy(() -> doseLog(pet, othersCourse, null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingACourseKeepsItsLogsAsPlainMedicineLogs() {
        UUID[] pet = ownerAndPet();
        UUID course = course(pet[1], "DAILY", null, null);
        doseLog(pet, course, LocalDate.of(2026, 10, 1), 480);

        this.jdbc.update("delete from medication_course where id = ?", course);

        Integer unlinked = this.jdbc.queryForObject(
                "select count(*) from pet_log where pet_id = ? and course_id is null and dose_on is null and dose_minute is null",
                Integer.class, pet[1]);
        assertThat(unlinked).isEqualTo(1);
    }

    @Test
    void everyNDaysNeedsTheNumberOfDays() {
        UUID[] pet = ownerAndPet();

        assertThatThrownBy(() -> course(pet[1], "EVERY_N_DAYS", null, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void maxPerDayBelongsToAsNeededOnly() {
        UUID[] pet = ownerAndPet();

        assertThatThrownBy(() -> course(pet[1], "DAILY", null, 2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID[] ownerAndPet() {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        UUID owner = seed.user();
        return new UUID[]{owner, seed.pet(owner)};
    }

    private UUID course(UUID petId, String schedule, Integer everyDays, Integer maxPerDay) {
        UUID id = UUID.randomUUID();
        this.jdbc.update("""
                insert into medication_course (id, pet_id, client_id, name, amount_hundredths, unit, schedule, every_days,
                    with_food, starts_on, start_minute, max_per_day, created_at, updated_at)
                values (?, ?, ?, 'Amoxicillin', 100, 'TABLET', ?, ?, 'ANY', ?, 480, ?, now(), now())
                """, id, petId, UUID.randomUUID(), schedule, everyDays, LocalDate.of(2026, 10, 1), maxPerDay);
        return id;
    }

    private void doseLog(UUID[] pet, UUID courseId, LocalDate doseOn, Integer doseMinute) {
        this.jdbc.update("""
                insert into pet_log (id, pet_id, client_id, type, local_date, minute_of_day, logged_by, created_at,
                    occurred_at, utc_offset_minutes, course_id, dose_on, dose_minute)
                values (?, ?, ?, 'MEDICINE', ?, 480, ?, now(), now(), 120, ?, ?, ?)
                """, UUID.randomUUID(), pet[1], UUID.randomUUID(), LocalDate.of(2026, 10, 1), pet[0], courseId, doseOn, doseMinute);
    }
}
