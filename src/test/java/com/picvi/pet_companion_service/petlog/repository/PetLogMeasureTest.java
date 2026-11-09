package com.picvi.pet_companion_service.petlog.repository;

import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.dto.PetLogDto;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PetLogMeasureTest extends PostgresTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 10);

    @Autowired
    private PetLogRepository repository;
    @Autowired
    private PetRepository petRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManager entityManager;

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
    void everyNumberIsStoredAsItsOwnMeasureAndComesBack() {
        PetLog saved = this.save(480, new LogValues(null, 6, null, 40, 4200, 30, null, null, null, null));
        this.entityManager.flush();
        this.entityManager.clear();

        PetLog reloaded = this.repository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStoolScore()).isEqualTo(6);
        assertThat(reloaded.getAmountGrams()).isEqualTo(40);
        assertThat(reloaded.getWeightGrams()).isEqualTo(4200);
        assertThat(reloaded.getDurationMinutes()).isEqualTo(30);
        assertThat(this.jdbc.queryForList("select kind from pet_log_measure where log_id = ? order by kind", String.class, saved.getId()))
                .containsExactly("AMOUNT_GRAMS", "DURATION_MINUTES", "STOOL_SCORE", "WEIGHT_GRAMS");
    }

    @Test
    void logWithoutNumbersHasNoMeasureRows() {
        PetLog saved = this.save(480, LogValues.none());
        this.entityManager.flush();

        assertThat(this.jdbc.queryForObject("select count(*) from pet_log_measure where log_id = ?", Integer.class, saved.getId())).isZero();
    }

    @Test
    void editingRemovesANumberThatIsNoLongerSet() {
        PetLog saved = this.save(480, new LogValues(null, 6, null, null, null, null, null, null, null, null));
        this.entityManager.flush();

        saved.replace(null, new LogValues(null, null, null, null, null, null, null, null, null, "better"), DAY, 480, loggedAt(), Instant.parse("2026-10-10T12:00:00Z"));
        this.entityManager.flush();
        this.entityManager.clear();

        assertThat(this.jdbc.queryForObject("select count(*) from pet_log_measure where log_id = ?", Integer.class, saved.getId())).isZero();
        assertThat(this.repository.findById(saved.getId()).orElseThrow().getStoolScore()).isNull();
    }

    @Test
    void editingChangesAnExistingNumber() {
        PetLog saved = this.save(480, new LogValues(null, null, null, null, 4200, null, null, null, null, null));
        this.entityManager.flush();

        saved.replace(null, new LogValues(null, null, null, null, 4300, null, null, null, null, null), DAY, 480, loggedAt(), Instant.parse("2026-10-10T12:00:00Z"));
        this.entityManager.flush();
        this.entityManager.clear();

        assertThat(this.repository.findById(saved.getId()).orElseThrow().getWeightGrams()).isEqualTo(4300);
    }

    @Test
    void deletingALogDeletesItsMeasures() {
        PetLog saved = this.save(480, new LogValues(null, 6, null, null, null, null, null, null, null, null));
        this.entityManager.flush();

        this.repository.delete(saved);
        this.entityManager.flush();

        assertThat(this.jdbc.queryForObject("select count(*) from pet_log_measure where log_id = ?", Integer.class, saved.getId())).isZero();
    }

    @Test
    void loadingAWindowOfLogsDoesNotQueryOncePerLog() {
        IntStream.range(0, 30).forEach(index -> this.save(100 + index, new LogValues(null, 5, Set.of(LogFlag.BLOOD), null, 4000 + index, null, null, null, null, null)));
        this.entityManager.flush();
        this.entityManager.clear();
        Statistics statistics = this.entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        List<PetLogDto> window = this.repository.findAllByPetIdAndLocalDateBetweenOrderByLocalDateAscMinuteOfDayAsc(this.petId, DAY.minusDays(13), DAY)
                .stream().map(PetLogDto::of).toList();

        assertThat(window).hasSize(30).allSatisfy(log -> assertThat(log.stoolScore()).isEqualTo(5));
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(5);
    }

    private PetLog save(int minute, LogValues values) {
        return this.repository.save(new PetLog(this.pet, UUID.randomUUID(), LogType.TOILET, this.userId, null, values, DAY, minute, loggedAt(), null));
    }

    private static LoggedAt loggedAt() {
        return new LoggedAt(OffsetDateTime.parse(DAY + "T10:00+02:00"), 120);
    }
}
