package com.picvi.pet_companion_service.medication;

import com.picvi.pet_companion_service.exception.medication.CourseHasDoses;
import com.picvi.pet_companion_service.exception.petlog.InvalidCourseDose;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import com.picvi.pet_companion_service.medication.dto.CourseDto;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import com.picvi.pet_companion_service.medication.dto.PlannedDoseDto;
import com.picvi.pet_companion_service.medication.dto.StopCourseRequest;
import com.picvi.pet_companion_service.medication.service.MedicationService;
import com.picvi.pet_companion_service.petlog.domain.CourseDose;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import com.picvi.pet_companion_service.petlog.domain.LogValues;
import com.picvi.pet_companion_service.petlog.domain.NewLog;
import com.picvi.pet_companion_service.petlog.service.PetLogService;
import com.picvi.pet_companion_service.support.DatabaseSeed;
import com.picvi.pet_companion_service.support.PostgresTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicationFlowTest extends PostgresTest {
    private static final int MORNING = 480;
    private static final int EVENING = 1080;

    @Autowired
    private MedicationService medicationService;
    @Autowired
    private PetLogService petLogService;
    @Autowired
    private JdbcTemplate jdbc;
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
    void givingADoseMarksItInTheListAndCountsIt() {
        CourseDto course = this.startCourse();
        PlannedDoseDto first = course.doses().getFirst();

        this.give(course, first.on(), first.minute());

        CourseDto listed = this.medicationService.list(this.owner, this.pet, this.today).courses().getFirst();
        assertThat(listed.givenDoses()).isEqualTo(1);
        assertThat(listed.doses().getFirst().logId()).isNotNull();
        assertThat(listed.lastGiven().minute()).isEqualTo(first.minute());
    }

    @Test
    void givingTheSamePlannedDoseAgainReturnsTheFirstLog() {
        CourseDto course = this.startCourse();
        PlannedDoseDto first = course.doses().getFirst();

        PetLogService.LogResult one = this.give(course, first.on(), first.minute());
        PetLogService.LogResult again = this.give(course, first.on(), first.minute());

        assertThat(one.created()).isTrue();
        assertThat(again.created()).isFalse();
        assertThat(again.log().id()).isEqualTo(one.log().id());
    }

    @Test
    void twoPhonesGivingTheSameDoseAtOnceCreateOneLog() throws Exception {
        CourseDto course = this.startCourse();
        PlannedDoseDto first = course.doses().getFirst();
        Callable<PetLogService.LogResult> phone = () -> this.give(course, first.on(), first.minute());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<PetLogService.LogResult>> results = pool.invokeAll(List.of(phone, phone));
        pool.shutdown();

        long created = 0;
        for (Future<PetLogService.LogResult> result : results) {
            created += result.get().created() ? 1 : 0;
        }
        assertThat(created).isEqualTo(1);
        Integer rows = this.jdbc.queryForObject("select count(*) from pet_log where course_id = ?", Integer.class, course.id());
        assertThat(rows).isEqualTo(1);
    }

    @Test
    void aTimeThatIsNotPlannedIsRefused() {
        CourseDto course = this.startCourse();

        assertThatThrownBy(() -> this.give(course, this.today, 501)).isInstanceOf(InvalidCourseDose.class);
    }

    @Test
    void aMedicineWithoutAPlannedTimeBelongsToTheCourseToo() {
        CourseDto course = this.startCourse();

        this.give(course, null, null);

        assertThat(this.medicationService.get(this.owner, this.pet, course.id(), this.today).givenDoses()).isEqualTo(1);
    }

    @Test
    void aCourseWithDosesCanBeStoppedButNotDeleted() {
        CourseDto course = this.startCourse();
        this.give(course, null, null);

        assertThatThrownBy(() -> this.medicationService.delete(this.owner, this.pet, course.id())).isInstanceOf(CourseHasDoses.class);

        CourseDto stopped = this.medicationService.stop(this.owner, this.pet, course.id(), new StopCourseRequest(this.today, EVENING));
        assertThat(stopped.status()).isEqualTo(CourseStatus.STOPPED);
    }

    @Test
    void aCourseWithoutDosesCanBeDeleted() {
        CourseDto course = this.startCourse();

        this.medicationService.delete(this.owner, this.pet, course.id());

        assertThat(this.medicationService.list(this.owner, this.pet, this.today).courses()).isEmpty();
    }

    @Test
    void aCourseOfAnotherPetIsNeverListed() {
        this.startCourse();
        DatabaseSeed seed = new DatabaseSeed(this.jdbc);
        UUID otherOwner = seed.user();
        UUID otherPet = seed.pet(otherOwner);

        assertThat(this.medicationService.list(otherOwner, otherPet, this.today).courses()).isEmpty();
    }

    private CourseDto startCourse() {
        CreateCourseRequest request = new CreateCourseRequest(UUID.randomUUID(), this.today, "Amoxicillin", 100, DoseUnit.TABLET,
                DoseSchedule.DAILY, null, List.of(MORNING, EVENING), WithFood.YES, this.today, 0, this.today.plusDays(3), 0, true);
        return this.medicationService.create(this.owner, this.pet, request).course();
    }

    private PetLogService.LogResult give(CourseDto course, LocalDate plannedOn, Integer plannedMinute) {
        LogValues values = new LogValues(null, null, null, null, null, null, null, course.name(), "1 tablet", null);
        NewLog log = new NewLog(UUID.randomUUID(), LogType.MEDICINE, null, values, this.today, MORNING, 120,
                CourseDose.of(course.id(), plannedOn, plannedMinute));
        return this.petLogService.log(this.owner, this.pet, log, this.today);
    }
}
