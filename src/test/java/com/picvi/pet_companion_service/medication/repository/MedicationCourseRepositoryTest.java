package com.picvi.pet_companion_service.medication.repository;

import com.picvi.pet_companion_service.medication.domain.CourseInputBuilder;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.picvi.pet_companion_service.medication.domain.CoursePlans.at;
import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class MedicationCourseRepositoryTest extends PostgresTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Autowired
    private MedicationCourseRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID petId;

    @BeforeEach
    void setUp() {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        this.petId = seed.pet(seed.user());
    }

    @Test
    void aSavedCourseKeepsItsDoseTimes() {
        MedicationCourse saved = this.save(new CourseInputBuilder(TODAY).times(List.of(at(8), at(18))));

        this.repository.flush();

        assertThat(this.repository.findByIdAndPetId(saved.getId(), this.petId).orElseThrow().getTimes())
                .containsExactlyInAnyOrder(at(8), at(18));
    }

    @Test
    void aCourseIsOnlyFoundForItsOwnPet() {
        MedicationCourse saved = this.save(new CourseInputBuilder(TODAY));

        assertThat(this.repository.findByIdAndPetId(saved.getId(), UUID.randomUUID())).isEmpty();
    }

    @Test
    void theSameClientIdFindsTheSameCourse() {
        UUID clientId = UUID.randomUUID();
        MedicationCourse saved = this.repository.saveAndFlush(
                new MedicationCourse(this.petId, clientId, new CourseInputBuilder(TODAY).build(), true, Instant.EPOCH));

        assertThat(this.repository.findByPetIdAndClientId(this.petId, clientId)).contains(saved);
    }

    @Test
    void listingSkipsCoursesThatEndedBeforeTheWindow() {
        MedicationCourse running = this.save(new CourseInputBuilder(TODAY));
        this.save(new CourseInputBuilder(TODAY.minusDays(30)).ends(TODAY.minusDays(20), at(8)));
        MedicationCourse justEnded = this.save(new CourseInputBuilder(TODAY.minusDays(10)).ends(TODAY.minusDays(5), at(8)));

        assertThat(this.repository.findNotEndedBefore(this.petId, TODAY.minusDays(7)))
                .containsExactlyInAnyOrder(running, justEnded);
    }

    private MedicationCourse save(CourseInputBuilder input) {
        return this.repository.save(new MedicationCourse(this.petId, UUID.randomUUID(), input.build(), true, Instant.EPOCH));
    }
}
