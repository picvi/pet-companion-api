package com.picvi.pet_companion_service.medication.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.picvi.pet_companion_service.medication.domain.CoursePlans.at;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.daily;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.oct;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.stoppedAt;
import static org.assertj.core.api.Assertions.assertThat;

class CourseStatusRuleTest {
    private final CourseStatusRule rule = new CourseStatusRule();
    private final CoursePlan sevenDays = daily(List.of(at(8)), oct(18), at(8), oct(25), at(8));

    @Test
    void beforeTheEndMomentItIsActive() {
        assertThat(this.rule.status(this.sevenDays, oct(25), at(7))).isEqualTo(CourseStatus.ACTIVE);
    }

    @Test
    void atTheEndMomentItIsFinished() {
        assertThat(this.rule.status(this.sevenDays, oct(25), at(8))).isEqualTo(CourseStatus.FINISHED);
    }

    @Test
    void withoutAnEndItStaysActive() {
        CoursePlan openEnded = daily(List.of(at(8)), oct(18), at(8), null, null);

        assertThat(this.rule.status(openEnded, oct(30), at(8))).isEqualTo(CourseStatus.ACTIVE);
    }

    @Test
    void stoppingWinsOverEverythingElse() {
        CoursePlan stopped = stoppedAt(this.sevenDays, oct(20), at(12));

        assertThat(this.rule.status(stopped, oct(30), at(8))).isEqualTo(CourseStatus.STOPPED);
    }
}
