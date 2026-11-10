package com.picvi.pet_companion_service.medication.service;

import com.picvi.pet_companion_service.common.ClientDateValidator;
import com.picvi.pet_companion_service.exception.medication.CourseHasDoses;
import com.picvi.pet_companion_service.exception.medication.CourseNotFound;
import com.picvi.pet_companion_service.exception.medication.InvalidCourse;
import com.picvi.pet_companion_service.exception.nutrition.VersionConflict;
import com.picvi.pet_companion_service.medication.domain.CourseInputBuilder;
import com.picvi.pet_companion_service.medication.domain.CourseRules;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.CourseStatusRule;
import com.picvi.pet_companion_service.medication.domain.DoseSchedule;
import com.picvi.pet_companion_service.medication.domain.DoseUnit;
import com.picvi.pet_companion_service.medication.domain.WithFood;
import com.picvi.pet_companion_service.medication.domain.DoseTimesCalculator;
import com.picvi.pet_companion_service.medication.dto.CourseDto;
import com.picvi.pet_companion_service.medication.dto.CoursesResponse;
import com.picvi.pet_companion_service.medication.dto.CreateCourseRequest;
import com.picvi.pet_companion_service.medication.dto.StopCourseRequest;
import com.picvi.pet_companion_service.medication.dto.UpdateCourseRequest;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.pet.service.PetService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.CourseDoseGiven;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.CourseDoseTotals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.picvi.pet_companion_service.medication.domain.CoursePlans.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {
    private static final UUID OWNER = UUID.randomUUID();
    private static final UUID PET = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
    private static final UUID CLIENT = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-18T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ClientDateValidator clientDateValidator;
    @Mock
    private PetService petService;
    @Mock
    private MedicationCourseRepository repository;
    @Mock
    private PetLogApi petLogApi;

    private MedicationService service;

    @BeforeEach
    void setUp() {
        CourseAssembler assembler = new CourseAssembler(new DoseTimesCalculator(), new CourseStatusRule(), this.petLogApi);
        this.service = new MedicationService(this.clientDateValidator, this.petService, this.repository, new CourseRules(),
                assembler, this.petLogApi, CLOCK);
    }

    @Test
    void createsACourseAndShowsItsPlannedDoses() {
        when(this.repository.findByPetIdAndClientId(PET, CLIENT)).thenReturn(Optional.empty());
        when(this.repository.saveAndFlush(any(MedicationCourse.class))).thenAnswer(call -> withId(call.getArgument(0)));

        MedicationService.CreateResult result = this.service.create(OWNER, PET, createRequest());

        assertThat(result.created()).isTrue();
        assertThat(result.course().name()).isEqualTo("Amoxicillin");
        assertThat(result.course().status()).isEqualTo(CourseStatus.ACTIVE);
        assertThat(result.course().totalDays()).isEqualTo(7);
        assertThat(result.course().doses()).hasSize(14);
        assertThat(result.course().givenDoses()).isZero();
    }

    @Test
    void retryingACreateReturnsTheSameCourseWithoutSavingAgain() {
        when(this.repository.findByPetIdAndClientId(PET, CLIENT)).thenReturn(Optional.of(this.course()));

        MedicationService.CreateResult result = this.service.create(OWNER, PET, createRequest());

        assertThat(result.created()).isFalse();
        verify(this.repository, never()).saveAndFlush(any());
    }

    @Test
    void anInvalidCourseIsNotSaved() {
        when(this.repository.findByPetIdAndClientId(PET, CLIENT)).thenReturn(Optional.empty());
        CreateCourseRequest invalid = new CreateCourseRequest(CLIENT, TODAY, " ", 100, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> this.service.create(OWNER, PET, invalid)).isInstanceOf(InvalidCourse.class);

        verify(this.repository, never()).saveAndFlush(any());
    }

    @Test
    void theListShowsGivenDosesAndCountsThem() {
        MedicationCourse course = this.course();
        UUID logId = UUID.randomUUID();
        when(this.repository.findNotEndedBefore(PET, TODAY.minusDays(7))).thenReturn(List.of(course));
        when(this.petLogApi.courseDosesBetween(any(), any(), any())).thenReturn(
                Map.of(course.getId(), List.of(new CourseDoseGiven(logId, TODAY, 1085, TODAY, at(18)))));
        when(this.petLogApi.courseDoseTotals(any())).thenReturn(Map.of(course.getId(), new CourseDoseTotals(1, TODAY, 1085)));

        CoursesResponse response = this.service.list(OWNER, PET, TODAY);

        CourseDto dto = response.courses().getFirst();
        assertThat(dto.givenDoses()).isEqualTo(1);
        assertThat(dto.lastGiven().minute()).isEqualTo(1085);
        assertThat(dto.doses()).filteredOn(dose -> dose.logId() != null).hasSize(1);
    }

    @Test
    void updatingWithAStaleVersionIsAConflict() {
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(this.course()));

        assertThatThrownBy(() -> this.service.update(OWNER, PET, COURSE, 5, updateRequest(TODAY, at(18))))
                .isInstanceOf(VersionConflict.class);
    }

    @Test
    void theStartCanChangeUntilADoseIsGiven() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.petLogApi.courseDoseTotals(any())).thenReturn(Map.of());
        when(this.repository.saveAndFlush(course)).thenReturn(course);

        this.service.update(OWNER, PET, COURSE, 0, updateRequest(TODAY.plusDays(1), at(8)));

        assertThat(course.getStartsOn()).isEqualTo(TODAY.plusDays(1));
        assertThat(course.getStartMinute()).isEqualTo(at(8));
    }

    @Test
    void afterADoseWasGivenTheStoredStartStays() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.petLogApi.courseDoseTotals(any())).thenReturn(Map.of(COURSE, new CourseDoseTotals(1, TODAY, 1085)));
        when(this.repository.saveAndFlush(course)).thenReturn(course);

        this.service.update(OWNER, PET, COURSE, 0, updateRequest(TODAY.plusDays(1), at(8)));

        assertThat(course.getStartsOn()).isEqualTo(TODAY);
        assertThat(course.getStartMinute()).isEqualTo(at(18));
    }

    @Test
    void stoppingKeepsTheFirstStop() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.repository.saveAndFlush(course)).thenReturn(course);

        this.service.stop(OWNER, PET, COURSE, new StopCourseRequest(TODAY, at(20)));
        CourseDto second = this.service.stop(OWNER, PET, COURSE, new StopCourseRequest(TODAY.plusDays(1), at(9)));

        assertThat(second.stoppedOn()).isEqualTo(TODAY);
        assertThat(second.stoppedMinute()).isEqualTo(at(20));
        assertThat(second.status()).isEqualTo(CourseStatus.STOPPED);
    }

    @Test
    void aCourseOfAnotherPetIsNotFound() {
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.service.get(OWNER, PET, COURSE, TODAY)).isInstanceOf(CourseNotFound.class);
        assertThatThrownBy(() -> this.service.stop(OWNER, PET, COURSE, new StopCourseRequest(TODAY, at(8)))).isInstanceOf(CourseNotFound.class);
    }

    @Test
    void aCourseWithGivenDosesCannotBeDeleted() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.petLogApi.courseDoseTotals(List.of(COURSE))).thenReturn(Map.of(COURSE, new CourseDoseTotals(1, TODAY, 1085)));

        assertThatThrownBy(() -> this.service.delete(OWNER, PET, COURSE)).isInstanceOf(CourseHasDoses.class);

        verify(this.repository, never()).delete(any());
    }

    @Test
    void aCourseWithoutDosesIsDeleted() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.petLogApi.courseDoseTotals(List.of(COURSE))).thenReturn(Map.of());

        this.service.delete(OWNER, PET, COURSE);

        verify(this.repository).delete(course);
    }

    @Test
    void deletingAMissingCourseDoesNothing() {
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.empty());

        this.service.delete(OWNER, PET, COURSE);

        verify(this.repository, never()).delete(any());
    }

    @Test
    void aCourseWithDosesIsOnlyStoppedWhenItsStartIsDiscarded() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.petLogApi.courseDoseTotals(List.of(COURSE))).thenReturn(Map.of(COURSE, new CourseDoseTotals(1, TODAY, 1085)));

        this.service.stopOrDelete(OWNER, PET, COURSE, TODAY, 600);

        assertThat(course.getStoppedOn()).isEqualTo(TODAY);
        verify(this.repository, never()).delete(any());
    }

    @Test
    void aCourseWithoutDosesIsDeletedWhenItsStartIsDiscarded() {
        MedicationCourse course = this.course();
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
        when(this.petLogApi.courseDoseTotals(List.of(COURSE))).thenReturn(Map.of());

        this.service.stopOrDelete(OWNER, PET, COURSE, TODAY, 600);

        verify(this.repository).delete(course);
    }

    @Test
    void discardingAMissingCourseDoesNothing() {
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.empty());

        this.service.stopOrDelete(OWNER, PET, COURSE, TODAY, 600);

        verify(this.repository, never()).delete(any());
    }

    private MedicationCourse course() {
        return withId(new MedicationCourse(PET, CLIENT, new CourseInputBuilder(TODAY).build(), true, CLOCK.instant()));
    }

    private static MedicationCourse withId(MedicationCourse course) {
        ReflectionTestUtils.setField(course, "id", COURSE);
        return course;
    }

    private static CreateCourseRequest createRequest() {
        return new CreateCourseRequest(CLIENT, TODAY, "Amoxicillin", 100, DoseUnit.TABLET, DoseSchedule.DAILY,
                null, List.of(at(8), at(18)), WithFood.YES, TODAY, at(18), TODAY.plusDays(7), at(18), true);
    }

    private static UpdateCourseRequest updateRequest(LocalDate startsOn, int startMinute) {
        return new UpdateCourseRequest(TODAY, "Amoxicillin", 100, DoseUnit.TABLET, DoseSchedule.DAILY,
                null, List.of(at(8), at(18)), WithFood.YES, startsOn, startMinute, TODAY.plusDays(10), at(18), true);
    }
}
