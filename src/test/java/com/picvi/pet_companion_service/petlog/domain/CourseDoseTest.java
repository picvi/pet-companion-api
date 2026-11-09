package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseDoseTest {
    private static final UUID COURSE = UUID.randomUUID();
    private static final LocalDate DAY = LocalDate.of(2026, 10, 18);

    @Test
    void noCourseMeansNoCourseDose() {
        assertThat(CourseDose.of(null, null, null)).isNull();
    }

    @Test
    void aDoseWithoutAPlannedTimeBelongsToTheCourseOnly() {
        CourseDose dose = CourseDose.of(COURSE, null, null);

        assertThat(dose.courseId()).isEqualTo(COURSE);
        assertThat(dose.answersPlannedTime()).isFalse();
    }

    @Test
    void aPlannedDoseCarriesItsTime() {
        CourseDose dose = CourseDose.of(COURSE, DAY, 480);

        assertThat(dose.answersPlannedTime()).isTrue();
        assertThat(dose.plannedOn()).isEqualTo(DAY);
        assertThat(dose.plannedMinute()).isEqualTo(480);
    }

    @Test
    void aPlannedTimeWithoutACourseIsRejected() {
        assertThatThrownBy(() -> CourseDose.of(null, DAY, 480)).isInstanceOf(InvalidLog.class)
                .hasMessage("courseDose: INVALID");
    }

    @Test
    void halfAPlannedTimeIsRejected() {
        assertThatThrownBy(() -> CourseDose.of(COURSE, DAY, null)).isInstanceOf(InvalidLog.class);
        assertThatThrownBy(() -> CourseDose.of(COURSE, null, 480)).isInstanceOf(InvalidLog.class);
    }
}
