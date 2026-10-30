package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.entity.FoodProduct;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class FoodProductSearchTest extends PostgresTest {
    @Autowired
    private FoodProductRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void formFilterKeepsOnlyThatForm() {
        UUID user = new DatabaseSeed(this.jdbc).user();

        List<FoodProduct> found = this.repository.search(user, "MAIN", "WET", null, "", "", 50, 0);

        assertThat(found).isNotEmpty().allSatisfy(p -> assertThat(p.getForm().name()).isEqualTo("WET"));
    }

    @Test
    void lifeStageFilterAlsoKeepsAllStageProducts() {
        UUID user = new DatabaseSeed(this.jdbc).user();

        List<FoodProduct> found = this.repository.search(user, "MAIN", null, "PUPPY", "", "", 500, 0);

        assertThat(found).isNotEmpty().allSatisfy(p -> assertThat(p.getLifeStage().name()).isIn("PUPPY", "ALL"));
        assertThat(found).extracting(p -> p.getLifeStage().name()).contains("PUPPY");
    }
}
