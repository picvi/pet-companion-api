package com.picvi.pet_companion_service.nutrition.repository;

import com.picvi.pet_companion_service.nutrition.domain.TriedReaction;
import com.picvi.pet_companion_service.nutrition.entity.PetTriedFood;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
class PetTriedFoodRepositoryTest extends PostgresTest {
    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    @Autowired
    private PetTriedFoodRepository repository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

    private UUID userId;
    private UUID petId;

    @BeforeEach
    void setUp() {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        this.userId = seed.user();
        this.petId = seed.pet(this.userId);
    }

    @Test
    void aTriedFoodRoundTrips() {
        this.repository.saveAndFlush(new PetTriedFood(this.petId, "CARROT", LocalDate.of(2026, 10, 12), TriedReaction.LOVED_IT, "crunchy", this.userId, NOW));
        this.entityManager.clear();

        PetTriedFood reloaded = this.repository.findByPetIdAndFoodCode(this.petId, "CARROT").orElseThrow();

        assertThat(reloaded.getTriedOn()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(reloaded.getReaction()).isEqualTo(TriedReaction.LOVED_IT);
        assertThat(reloaded.getNote()).isEqualTo("crunchy");
        assertThat(reloaded.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void theNewestTryComesFirst() {
        this.repository.save(new PetTriedFood(this.petId, "CARROT", LocalDate.of(2026, 10, 1), TriedReaction.FINE, null, this.userId, NOW));
        this.repository.save(new PetTriedFood(this.petId, "APPLE", LocalDate.of(2026, 10, 9), TriedReaction.FINE, null, this.userId, NOW));
        this.repository.saveAndFlush(new PetTriedFood(this.petId, "BANANA", LocalDate.of(2026, 10, 5), TriedReaction.NOT_INTO_IT, null, this.userId, NOW));

        List<String> order = this.repository.findAllByPetIdOrderByTriedOnDescFoodCodeAsc(this.petId).stream().map(PetTriedFood::getFoodCode).toList();

        assertThat(order).containsExactly("APPLE", "BANANA", "CARROT");
    }

    @Test
    void aFoodOutsideTheSafetyListIsRejectedByTheDatabase() {
        assertThatThrownBy(() -> this.repository.saveAndFlush(
                new PetTriedFood(this.petId, "NOT_A_FOOD", LocalDate.of(2026, 10, 1), TriedReaction.FINE, null, this.userId, NOW)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingTheUserKeepsTheOwnersEntry() {
        UUID contributor = new DatabaseSeed(this.jdbc).user();
        this.repository.saveAndFlush(new PetTriedFood(this.petId, "CARROT", LocalDate.of(2026, 10, 1), TriedReaction.FINE, null, contributor, NOW));

        this.jdbc.update("delete from users where id = ?", contributor);
        this.entityManager.clear();

        PetTriedFood kept = this.repository.findByPetIdAndFoodCode(this.petId, "CARROT").orElseThrow();
        assertThat(kept.getCreatedBy()).isNull();
    }
}
