package com.picvi.pet_companion_service.medication.api;

import com.picvi.pet_companion_service.exception.petlog.InvalidCourseDose;
import com.picvi.pet_companion_service.medication.domain.CourseInputBuilder;
import com.picvi.pet_companion_service.medication.domain.DoseTimesCalculator;
import com.picvi.pet_companion_service.medication.entity.MedicationCourse;
import com.picvi.pet_companion_service.medication.repository.MedicationCourseRepository;
import com.picvi.pet_companion_service.petlog.domain.CourseDose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseDoseCheckAdapterTest {
    private static final UUID PET = UUID.randomUUID();
    private static final UUID COURSE = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 10, 18);

    @Mock
    private MedicationCourseRepository repository;

    private CourseDoseCheckAdapter adapter;

    @BeforeEach
    void setUp() {
        this.adapter = new CourseDoseCheckAdapter(this.repository, new DoseTimesCalculator());
    }

    @Test
    void aPlannedDoseOfTheCourseIsAccepted() {
        this.courseExists();

        assertThatCode(() -> this.adapter.verify(PET, new CourseDose(COURSE, START.plusDays(1), 480))).doesNotThrowAnyException();
    }

    @Test
    void aDoseWithoutAPlannedTimeIsAlwaysAccepted() {
        this.courseExists();

        assertThatCode(() -> this.adapter.verify(PET, new CourseDose(COURSE, null, null))).doesNotThrowAnyException();
    }

    @Test
    void aTimeThatIsNotPlannedIsRejected() {
        this.courseExists();

        assertThatThrownBy(() -> this.adapter.verify(PET, new CourseDose(COURSE, START.plusDays(1), 500)))
                .isInstanceOf(InvalidCourseDose.class)
                .hasMessageStartingWith("plannedTime");
    }

    @Test
    void aCourseOfAnotherPetIsRejected() {
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> this.adapter.verify(PET, new CourseDose(COURSE, null, null)))
                .isInstanceOf(InvalidCourseDose.class)
                .hasMessageStartingWith("courseId");
    }

    private void courseExists() {
        MedicationCourse course = new MedicationCourse(PET, UUID.randomUUID(), new CourseInputBuilder(START).build(), true, Instant.EPOCH);
        when(this.repository.findByIdAndPetId(COURSE, PET)).thenReturn(Optional.of(course));
    }
}
