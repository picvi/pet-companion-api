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
