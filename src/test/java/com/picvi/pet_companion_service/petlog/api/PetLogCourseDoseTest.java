package com.picvi.pet_companion_service.petlog.api;

import com.picvi.pet_companion_service.pet.entity.Pet;
import com.picvi.pet_companion_service.pet.repository.PetRepository;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.CourseDoseTotals;
import com.picvi.pet_companion_service.petlog.domain.CourseDose;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.LoggedAt;
import com.picvi.pet_companion_service.petlog.entity.PetLog;
import com.picvi.pet_companion_service.petlog.repository.PetLogRepository;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class PetLogCourseDoseTest extends PostgresTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 18);

    @Autowired
    private PetLogApi api;
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
    void plannedDosesInTheWindowAreGroupedByCourse() {
        UUID course = this.course();
        UUID other = this.course();
        PetLog inside = this.give(course, DAY, 480, DAY, 485);
        this.give(course, DAY.plusDays(5), 480, DAY.plusDays(5), 480);
        this.give(other, DAY, 600, DAY, 600);
        this.give(course, null, null, DAY, 700);

        Map<UUID, List<PetLogApi.CourseDoseGiven>> doses = this.api.courseDosesBetween(List.of(course), DAY.minusDays(1), DAY.plusDays(2));

        assertThat(doses).containsOnlyKeys(course);
        assertThat(doses.get(course)).containsExactly(new PetLogApi.CourseDoseGiven(inside.getId(), DAY, 485, DAY, 480));
    }

    @Test
    void totalsCountEveryDoseAndKnowTheNewestOne() {
        UUID course = this.course();
        UUID untouched = this.course();
        this.give(course, DAY, 480, DAY, 480);
        this.give(course, DAY.plusDays(1), 485, DAY.plusDays(1), 480);
        this.give(course, null, null, DAY.plusDays(1), 900);

        Map<UUID, CourseDoseTotals> totals = this.api.courseDoseTotals(List.of(course, untouched));

        assertThat(totals).containsOnlyKeys(course);
        assertThat(totals.get(course)).isEqualTo(new CourseDoseTotals(3, DAY.plusDays(1), 900));
    }

    @Test
    void askingAboutNoCoursesAnswersNothing() {
        assertThat(this.api.courseDosesBetween(List.of(), DAY, DAY)).isEmpty();
        assertThat(this.api.courseDoseTotals(List.of())).isEmpty();
    }

    @Test
    void theSamePlannedDoseCannotBeLoggedTwice() {
        UUID course = this.course();
        this.give(course, DAY, 480, DAY, 480);

        assertThat(this.repository.findByCourseIdAndDoseOnAndDoseMinute(course, DAY, 480)).isPresent();
    }

    private UUID course() {
        UUID id = UUID.randomUUID();
        this.jdbc.update("""
                insert into medication_course (id, pet_id, client_id, name, amount_hundredths, unit, schedule,
                    with_food, starts_on, start_minute, created_at, updated_at)
                values (?, ?, ?, 'Amoxicillin', 100, 'TABLET', 'DAILY', 'ANY', ?, 480, now(), now())
                """, id, this.petId, UUID.randomUUID(), DAY);
        return id;
    }

    private PetLog give(UUID course, LocalDate plannedOn, Integer plannedMinute, LocalDate on, int minute) {
        LoggedAt loggedAt = new LoggedAt(OffsetDateTime.parse(on + "T10:00+02:00"), 120);
        CourseDose dose = new CourseDose(course, plannedOn, plannedMinute);
        return this.repository.saveAndFlush(new PetLog(this.pet, UUID.randomUUID(), LogType.MEDICINE, this.userId, null,
                LogValues.none(), on, minute, loggedAt, dose));
    }
}
