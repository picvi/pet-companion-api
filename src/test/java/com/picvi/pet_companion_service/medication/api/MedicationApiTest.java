package com.picvi.pet_companion_service.medication.api;

import com.picvi.pet_companion_service.medication.domain.CourseInputBuilder;
import com.picvi.pet_companion_service.medication.domain.CourseStatus;
import com.picvi.pet_companion_service.medication.domain.CourseStatusRule;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.medication.service.MedicationService;
import com.picvi.pet_companion_service.petlog.api.PetLogApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationApiTest {
    private static final UUID PET = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);

    @Mock
    private MedicationService service;
    @Mock
    private MedicationCourseRepository repository;
    @Mock
    private PetLogApi petLogApi;

    private MedicationApi api;

    @BeforeEach
    void setUp() {
        this.api = new MedicationApi(this.service, this.repository, new CourseStatusRule(), this.petLogApi);
    }

    @Test
    void coursesAreSummarisedWithTheirStatusOnTheDay() {
        MedicationCourse course = new MedicationCourse(PET, UUID.randomUUID(), new CourseInputBuilder(TODAY).build(), true, Instant.parse("2026-10-18T08:00:00Z"));
        ReflectionTestUtils.setField(course, "id", UUID.randomUUID());
        when(this.repository.findAllByPetIdAndIdIn(PET, List.of(course.getId()))).thenReturn(List.of(course));

        assertThat(this.api.courses(PET, List.of(course.getId()), TODAY))
                .containsExactly(new MedicationApi.CourseSummary(course.getId(), course.getName(), CourseStatus.ACTIVE));
    }

    @Test
    void coursesOfAPeriodCarryTheirDosesGiven() {
        MedicationCourse given = new MedicationCourse(PET, UUID.randomUUID(), new CourseInputBuilder(TODAY).build(), true, Instant.parse("2026-10-18T08:00:00Z"));
        MedicationCourse untouched = new MedicationCourse(PET, UUID.randomUUID(), new CourseInputBuilder(TODAY).name("Probiotic").build(), true, Instant.parse("2026-10-18T08:00:00Z"));
        ReflectionTestUtils.setField(given, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(untouched, "id", UUID.randomUUID());
        when(this.repository.findOverlapping(PET, TODAY, TODAY.plusDays(3))).thenReturn(List.of(given, untouched));
        when(this.petLogApi.courseDoseTotals(List.of(given.getId(), untouched.getId())))
                .thenReturn(Map.of(given.getId(), new PetLogApi.CourseDoseTotals(2, TODAY, 480)));

        assertThat(this.api.coursesBetween(PET, TODAY, TODAY.plusDays(3), TODAY))
                .extracting(MedicationApi.CourseInPeriod::name, MedicationApi.CourseInPeriod::givenDoses)
                .containsExactly(tuple("Amoxicillin", 2), tuple("Probiotic", 0));
    }

    @Test
    void askingForNoCoursesReadsNothing() {
        assertThat(this.api.courses(PET, List.of(), TODAY)).isEmpty();

        verifyNoInteractions(this.repository);
    }
}
