package com.picvi.pet_companion_service.medical;

import com.picvi.pet_companion_service.care.api.CareApi;
import com.picvi.pet_companion_service.medical.domain.VisitOutcome;
import com.picvi.pet_companion_service.medical.domain.VisitStatus;
import com.picvi.pet_companion_service.medical.domain.VisitTestKind;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.ClinicMedicine;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.HomeCourse;
import com.picvi.pet_companion_service.medical.dto.LogVisitRequest.TestRequest;
import com.picvi.pet_companion_service.medical.dto.VetVisitDto;
import com.picvi.pet_companion_service.medical.dto.VisitHistoryPage;
import com.picvi.pet_companion_service.medical.service.VetVisitService;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.petlog.api.PetLogRecorder;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class VetVisitFlowTest extends PostgresTest {
    private static final String CARE_CODE = "RABIES";

    @Autowired
    private VetVisitService visitService;
    @Autowired
    private CareApi careApi;
    @Autowired
    private PetLogRecorder petLogRecorder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private Clock clock;

    private UUID owner;
    private UUID pet;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        this.owner = seed.user();
        this.pet = seed.pet(this.owner);
        this.today = LocalDate.now(this.clock);
    }

    @Test
    void aVisitLinksEverythingThatHappenedInOtherModules() {
        VetVisitDto visit = this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today)).visit();

        assertThat(visit.status()).isEqualTo(VisitStatus.DONE);
        assertThat(visit.outcomes()).containsExactly(VisitOutcome.TREATMENT, VisitOutcome.TESTS, VisitOutcome.BACK);
        assertThat(visit.careEvents()).extracting("itemCode").containsExactly(CARE_CODE);
        assertThat(visit.clinicMedicines()).extracting("label").containsExactly("Convenia injection");
        assertThat(visit.weightGrams()).isEqualTo(4200);
        assertThat(visit.courses()).extracting("name", "status").containsExactly(tuple("Metronidazole", "ACTIVE"));
        assertThat(visit.followUpPlanId()).isNotNull();
        assertThat(this.count("pet_day_event where pet_id = ? and type = 'VET'")).isEqualTo(1);
    }

    @Test
    void aRetriedLogCreatesNothingTwice() {
        LogVisitRequest request = this.fullVisit(UUID.randomUUID(), this.today);

        VetVisitService.CreateResult first = this.visitService.log(this.owner, this.pet, request);
        VetVisitService.CreateResult again = this.visitService.log(this.owner, this.pet, request);

        assertThat(first.created()).isTrue();
        assertThat(again.created()).isFalse();
        assertThat(again.visit().id()).isEqualTo(first.visit().id());
        assertThat(this.count("care_event where pet_id = ?")).isEqualTo(1);
        assertThat(this.count("pet_log where pet_id = ?")).isEqualTo(2);
        assertThat(this.count("medication_course where pet_id = ?")).isEqualTo(1);
        assertThat(this.count("pet_vet_visit where pet_id = ?")).isEqualTo(2);
    }

    @Test
    void undoingTheCareEventOrDeletingTheLogRemovesThemFromTheVisit() {
        VetVisitDto visit = this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today)).visit();

        this.careApi.undo(this.owner, this.pet, visit.careEvents().getFirst().id(), this.today);
        this.petLogRecorder.delete(this.owner, this.pet, visit.clinicMedicines().getFirst().logId(), this.today);

        VetVisitDto reloaded = this.visitService.get(this.owner, this.pet, visit.id());
        assertThat(reloaded.careEvents()).isEmpty();
        assertThat(reloaded.clinicMedicines()).isEmpty();
        assertThat(reloaded.weightGrams()).isEqualTo(4200);
    }

    @Test
    void changingAVisitAddsAnItemAndLeavesTheOthersUntouched() {
        VetVisitDto visit = this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today)).visit();
        int logsBefore = this.count("pet_log where pet_id = ?");
        int coursesBefore = this.count("medication_course where pet_id = ?");

        VetVisitDto changed = this.visitService.update(this.owner, this.pet, visit.id(), visit.version(), new LogVisitRequest(
                null, this.today, this.today, 600, "VetCare", null, false, List.of("DHPPI"), List.of(), List.of(), null,
                List.of(), null, new LogVisitRequest.FollowUp(this.today.plusDays(7), 600)));

        assertThat(changed.careEvents()).extracting("itemCode").containsExactlyInAnyOrder(CARE_CODE, "DHPPI");
        assertThat(this.count("pet_log where pet_id = ?")).isEqualTo(logsBefore);
        assertThat(this.count("medication_course where pet_id = ?")).isEqualTo(coursesBefore);
    }

    @Test
    void deletingWithItsItemsRemovesThemAndKeepingThemLeavesThem() {
        VetVisitDto removed = this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today)).visit();
        VetVisitDto kept = this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today.minusDays(1))).visit();

        this.visitService.delete(this.owner, this.pet, removed.id(), true, this.today, 700);
        this.visitService.delete(this.owner, this.pet, kept.id(), false, this.today, 700);

        assertThat(this.count("care_event where pet_id = ?")).isEqualTo(1);
        assertThat(this.count("pet_log where pet_id = ?")).isEqualTo(2);
        assertThat(this.count("medication_course where pet_id = ?")).isEqualTo(1);
        assertThat(this.visitService.upcoming(this.owner, this.pet, this.today).lastVisit()).isNull();
    }

    @Test
    void theHistoryOfTwentyVisitsIsReadInAFewStatements() {
        IntStream.range(0, 20).forEach(index -> this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today.minusDays(index))));
        Statistics statistics = this.entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        VisitHistoryPage page = this.visitService.history(this.owner, this.pet, null, 20);

        assertThat(page.visits()).hasSize(20).allSatisfy(visit -> {
            assertThat(visit.careEvents()).hasSize(1);
            assertThat(visit.clinicMedicines()).hasSize(1);
            assertThat(visit.weightGrams()).isEqualTo(4200);
            assertThat(visit.courses()).extracting("status").containsExactly(CourseStatus.ACTIVE.name());
        });
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(12);
    }

    @Test
    void upcomingReadsNoCareEventsLogsOrCourses() {
        this.visitService.log(this.owner, this.pet, this.fullVisit(UUID.randomUUID(), this.today));
        Statistics statistics = this.entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        this.visitService.upcoming(this.owner, this.pet, this.today);

        assertThat(statistics.getQueries()).noneMatch(query -> query.contains("CareEvent") || query.contains("PetLog") || query.contains("MedicationCourse"));
    }

    private LogVisitRequest fullVisit(UUID clientId, LocalDate on) {
        HomeCourse course = new HomeCourse(UUID.randomUUID(), "Metronidazole", 50, null, null, null, List.of(480), null,
                this.today, 480, this.today.plusDays(7), 480, true);
        return new LogVisitRequest(clientId, this.today, on, 600, "VetCare", "Upset tummy", false, List.of(CARE_CODE),
                List.of(new ClinicMedicine("Convenia injection", "0.5 ml")), List.of(course), 4200,
                List.of(new TestRequest(VisitTestKind.BLOOD, true)), null,
                new LogVisitRequest.FollowUp(this.today.plusDays(7), 600));
    }

    private int count(String fromAndWhere) {
        return this.jdbc.queryForObject("select count(*) from " + fromAndWhere, Integer.class, this.pet);
    }
}
