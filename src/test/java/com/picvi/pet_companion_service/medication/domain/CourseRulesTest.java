package com.picvi.pet_companion_service.medication.domain;

import com.picvi.pet_companion_service.exception.medication.InvalidCourse;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.picvi.pet_companion_service.medication.domain.CoursePlans.at;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.oct;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CourseRulesTest {
    private static final LocalDate TODAY = oct(18);

    private final CourseRules rules = new CourseRules();

    @Test
    void aTwiceADayCourseWithAnEndIsValid() {
        assertThatCode(() -> this.rules.validate(builder().build(), TODAY)).doesNotThrowAnyException();
    }

    @Test
    void aBlankNameIsRequired() {
        this.assertRejected(builder().name("  "), "name", InvalidCourse.REQUIRED);
    }

    @Test
    void aTooLongNameIsRejected() {
        this.assertRejected(builder().name("x".repeat(81)), "name", InvalidCourse.TOO_LONG);
    }

    @Test
    void anAmountOutsideTheRangeIsRejected() {
        this.assertRejected(builder().amountHundredths(0), "amountHundredths", InvalidCourse.OUT_OF_RANGE);
    }

    @Test
    void aScheduledCourseNeedsAtLeastOneTime() {
        this.assertRejected(builder().times(List.of()), "times", InvalidCourse.REQUIRED);
    }

    @Test
    void duplicateTimesAreRejected() {
        this.assertRejected(builder().times(List.of(at(8), at(8))), "times", InvalidCourse.INVALID);
    }

    @Test
    void moreThanSixTimesAreRejected() {
        List<Integer> seven = new ArrayList<>();
        for (int hour = 6; hour < 13; hour++) {
            seven.add(at(hour));
        }

        this.assertRejected(builder().times(seven), "times", InvalidCourse.OUT_OF_RANGE);
    }

    @Test
    void aTimeAfterMidnightIsRejected() {
        this.assertRejected(builder().times(List.of(1440)), "times", InvalidCourse.OUT_OF_RANGE);
    }

    @Test
    void everyNDaysNeedsAnIntervalOfTwoToFourteen() {
        this.assertRejected(builder().schedule(DoseSchedule.EVERY_N_DAYS), "everyDays", InvalidCourse.REQUIRED);
        this.assertRejected(builder().schedule(DoseSchedule.EVERY_N_DAYS).everyDays(15), "everyDays", InvalidCourse.OUT_OF_RANGE);
    }

    @Test
    void anIntervalOnADailyCourseIsRejected() {
        this.assertRejected(builder().everyDays(3), "everyDays", InvalidCourse.INVALID);
    }

    @Test
    void aStartMoreThanThirtyDaysAwayIsRejected() {
        this.assertRejected(builder().startsOn(TODAY.plusDays(31)).ends(null, null), "startsOn", InvalidCourse.OUT_OF_RANGE);
    }

    @Test
    void anEndBeforeTheStartIsRejected() {
        this.assertRejected(builder().ends(TODAY, at(18)), "endsOn", InvalidCourse.OUT_OF_RANGE);
    }

    @Test
    void anEndDateWithoutAMinuteIsRejected() {
        this.assertRejected(builder().ends(TODAY.plusDays(7), null), "endsMinute", InvalidCourse.REQUIRED);
    }

    @Test
    void aCourseLongerThanAYearIsRejected() {
        this.assertRejected(builder().ends(TODAY.plusDays(366), at(8)), "endsOn", InvalidCourse.OUT_OF_RANGE);
    }

    private void assertRejected(CourseInputBuilder input, String field, String reason) {
        assertThatThrownBy(() -> this.rules.validate(input.build(), TODAY))
                .isInstanceOf(InvalidCourse.class)
                .hasMessage(field + ": " + reason);
    }

    private static CourseInputBuilder builder() {
        return new CourseInputBuilder(TODAY);
    }
}
