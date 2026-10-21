package com.picvi.pet_companion_service.db;

import com.picvi.pet_companion_service.account.service.AccountDeletionService;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDeletionCascadeTest extends PostgresTest {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AccountDeletionService accountDeletionService;

    private DatabaseSeed seed;

    @BeforeEach
    void setUp() {
        this.seed = new DatabaseSeed(this.jdbc);
    }

    @Test
    void ownerDeletionLeavesNothingAboutThePetOrTheUser() {
        UUID owner = this.seed.user();
        UUID pet = this.seed.pet(owner);
        this.seed.everythingAboutPet(pet, owner);
        assertThat(this.petTablesWithoutRows(pet)).as("tables the seed does not cover").isEmpty();

        this.accountDeletionService.delete(owner);

        assertThat(this.count("users", "id", owner)).isZero();
        assertThat(this.count("pets", "id", pet)).isZero();
        assertThat(this.count("refresh_tokens", "user_id", owner)).isZero();
        assertThat(this.petTablesWithRows(pet)).isEmpty();
    }

    @Test
    void deletingAContributorKeepsTheOwnersRecords() {
        UUID owner = this.seed.user();
        UUID contributor = this.seed.user();
        UUID pet = this.seed.pet(owner);
        UUID log = this.seed.log(pet, contributor);
        UUID condition = this.seed.condition(pet, contributor);
        UUID product = this.seed.foodProduct(contributor);
        UUID petFood = this.seed.petFood(pet, product, contributor);

        this.accountDeletionService.delete(contributor);

        assertThat(this.count("users", "id", contributor)).isZero();
        assertThat(this.nullContributor("pet_log", "logged_by", log)).isTrue();
        assertThat(this.nullContributor("pet_condition", "created_by", condition)).isTrue();
        assertThat(this.nullContributor("pet_food", "created_by", petFood)).isTrue();
        assertThat(this.nullContributor("food_product", "created_by", product)).isTrue();
    }

    private List<String> petTables() {
        return this.jdbc.queryForList("""
                select table_name from information_schema.columns
                where table_schema = 'public' and column_name = 'pet_id'
                  and table_name in (select table_name from information_schema.tables where table_type = 'BASE TABLE')
                order by table_name
                """, String.class);
    }

    private List<String> petTablesWithRows(UUID pet) {
        return this.petTables().stream().filter(table -> this.count(table, "pet_id", pet) > 0).toList();
    }

    private List<String> petTablesWithoutRows(UUID pet) {
        return this.petTables().stream().filter(table -> this.count(table, "pet_id", pet) == 0).toList();
    }

    private int count(String table, String column, UUID id) {
        return this.jdbc.queryForObject("select count(*) from " + table + " where " + column + " = ?", Integer.class, id);
    }

    private boolean nullContributor(String table, String column, UUID id) {
        return this.jdbc.queryForObject("select " + column + " is null from " + table + " where id = ?", Boolean.class, id);
    }
}
