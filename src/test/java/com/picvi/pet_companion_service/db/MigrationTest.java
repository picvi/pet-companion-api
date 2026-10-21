package com.picvi.pet_companion_service.db;

import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

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
}
