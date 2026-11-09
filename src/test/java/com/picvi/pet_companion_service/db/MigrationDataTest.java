package com.picvi.pet_companion_service.db;

import com.picvi.pet_companion_service.support.DatabaseSeed;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationDataTest {
    @Test
    void numbersOnOldPetLogsMoveIntoMeasuresWithoutLoss() throws IOException {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            this.flyway(dataSource, "27").migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            DatabaseSeed seed = new DatabaseSeed(jdbc);
            UUID user = seed.user();
            UUID pet = seed.pet(user);
            UUID everything = this.oldLog(jdbc, pet, user, "TOILET", "stool_score", 6, "duration_minutes", 30, "weight_grams", 4200, "amount_grams", 40);
            UUID onlyStool = this.oldLog(jdbc, pet, user, "TOILET", "stool_score", 3);
            UUID nothing = this.oldLog(jdbc, pet, user, "NOTE");

            this.flyway(dataSource, null).migrate();

            assertThat(this.measures(jdbc, everything)).containsEntry("STOOL_SCORE", 6).containsEntry("DURATION_MINUTES", 30)
                    .containsEntry("WEIGHT_GRAMS", 4200).containsEntry("AMOUNT_GRAMS", 40).hasSize(4);
            assertThat(this.measures(jdbc, onlyStool)).containsExactlyEntriesOf(Map.of("STOOL_SCORE", 3));
            assertThat(this.measures(jdbc, nothing)).isEmpty();
            assertThat(jdbc.queryForList("select column_name from information_schema.columns where table_name = 'pet_log'", String.class))
                    .doesNotContain("source", "stool_score", "weight_grams", "duration_minutes", "amount_grams")
                    .contains("accident", "wellbeing", "label", "dose", "note");
        }
    }

    @Test
    void aBookedNextCheckBecomesAPlannedVisitThatFollowsUpTheLoggedOne() throws IOException {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            this.flyway(dataSource, "37").migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            DatabaseSeed seed = new DatabaseSeed(jdbc);
            UUID pet = seed.pet(seed.user());
            UUID event = this.vetEvent(jdbc, pet, "2026-11-20", 540);
            UUID loggedVisit = this.oldVisit(jdbc, pet, "2026-11-01", "VetCare", event);

            this.flyway(dataSource, null).migrate();

            List<Map<String, Object>> visits = jdbc.queryForList(
                    "select id, status, scheduled_minute, follow_up_of, day_event_id, visited_on, clinic from pet_vet_visit where pet_id = ? order by status",
                    pet);
            assertThat(visits).hasSize(2);
            Map<String, Object> done = visits.get(0);
            Map<String, Object> planned = visits.get(1);
            assertThat(done).containsEntry("id", loggedVisit).containsEntry("status", "DONE").containsEntry("day_event_id", null);
            assertThat(planned).containsEntry("status", "PLANNED").containsEntry("scheduled_minute", 540)
                    .containsEntry("follow_up_of", loggedVisit).containsEntry("day_event_id", event)
                    .containsEntry("clinic", "VetCare");
            assertThat(planned.get("visited_on").toString()).isEqualTo("2026-11-20");
        }
    }

    @Test
    void aVetEventAddedStraightInHerDayBecomesAPlannedVisit() throws IOException {
        try (EmbeddedPostgres postgres = EmbeddedPostgres.start()) {
            DataSource dataSource = postgres.getPostgresDatabase();
            this.flyway(dataSource, "37").migrate();
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            DatabaseSeed seed = new DatabaseSeed(jdbc);
            UUID pet = seed.pet(seed.user());
            UUID event = this.vetEvent(jdbc, pet, "2026-11-20", 600);

            this.flyway(dataSource, null).migrate();

            Map<String, Object> visit = jdbc.queryForMap(
                    "select status, scheduled_minute, day_event_id, follow_up_of from pet_vet_visit where pet_id = ?", pet);
            assertThat(visit).containsEntry("status", "PLANNED").containsEntry("scheduled_minute", 600)
                    .containsEntry("day_event_id", event).containsEntry("follow_up_of", null);
        }
    }

    private UUID vetEvent(JdbcTemplate jdbc, UUID pet, String date, int minute) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into pet_day_event (id, pet_id, type, local_date, start_minute, duration_minutes, recurrence, plan_around, created_at)
                values (?, ?, 'VET', ?::date, ?, 60, 'NONE', true, now())
                """, id, pet, date, minute);
        return id;
    }

    private UUID oldVisit(JdbcTemplate jdbc, UUID pet, String visitedOn, String clinic, UUID event) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into pet_vet_visit (id, pet_id, client_id, visited_on, reason, outcomes, clinic, next_check_event_id, version, created_at, updated_at)
                values (?, ?, ?, ?::date, 'CHECK_UP', 'FINE', ?, ?, 0, now(), now())
                """, id, pet, UUID.randomUUID(), visitedOn, clinic, event);
        return id;
    }

    private Flyway flyway(DataSource dataSource, String target) {
        var configuration = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration");
        if (target != null) {
            configuration.target(target).repeatableSqlMigrationPrefix("SKIP_REPEATABLES");
        }
        return configuration.load();
    }

    private UUID oldLog(JdbcTemplate jdbc, UUID pet, UUID user, String type, Object... numbers) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into pet_log (id, pet_id, client_id, type, local_date, minute_of_day, source, logged_by, created_at, occurred_at, utc_offset_minutes)
                values (?, ?, ?, ?, '2026-10-01', 480, 'NOW_CARD', ?, now(), now(), 120)
                """, id, pet, UUID.randomUUID(), type, user);
        for (int index = 0; index < numbers.length; index += 2) {
            jdbc.update("update pet_log set " + numbers[index] + " = ? where id = ?", numbers[index + 1], id);
        }
        return id;
    }

    private Map<String, Integer> measures(JdbcTemplate jdbc, UUID log) {
        List<Map<String, Object>> rows = jdbc.queryForList("select kind, value from pet_log_measure where log_id = ?", log);
        return rows.stream().collect(java.util.stream.Collectors.toMap(row -> (String) row.get("kind"), row -> (Integer) row.get("value")));
    }
}
