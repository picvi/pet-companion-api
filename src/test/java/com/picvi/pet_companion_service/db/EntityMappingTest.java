package com.picvi.pet_companion_service.db;

import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@Transactional
class EntityMappingTest extends PostgresTest {
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void everyEntityLoadsFromTheMigratedSchema() {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        UUID user = seed.user();
        seed.everythingAboutPet(seed.pet(user), user);
        this.entityManager.flush();

        for (EntityType<?> entity : this.entityManager.getMetamodel().getEntities()) {
            assertThatCode(() -> this.entityManager.createQuery("select e from " + entity.getName() + " e", entity.getJavaType()).setMaxResults(20).getResultList())
                    .as(entity.getName())
                    .doesNotThrowAnyException();
        }
        assertThat(this.entityManager.getMetamodel().getEntities()).isNotEmpty();
    }
}
