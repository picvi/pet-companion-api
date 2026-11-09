package com.picvi.pet_companion_service.medication.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.picvi.pet_companion_service.medication.domain.CoursePlans.at;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.daily;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.everyDays;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.nov;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.oct;
import static com.picvi.pet_companion_service.medication.domain.CoursePlans.stoppedAt;
import static org.assertj.core.api.Assertions.assertThat;

class DoseTimesCalculatorTest {
    private final DoseTimesCalculator calculator = new DoseTimesCalculator();

    @Test
    void sevenDaysTwiceADayStartingInTheEveningGivesFourteenDoses() {
        CoursePlan plan = daily(List.of(at(8), at(18)), oct(18), at(18), oct(25), at(18));

        List<PlannedDose> doses = this.calculator.between(plan, oct(1), nov(30));

        assertThat(doses).hasSize(14);
        assertThat(doses.get(0)).isEqualTo(new PlannedDose(oct(18), at(18)));
        assertThat(doses.get(13)).isEqualTo(new PlannedDose(oct(25), at(8)));
        assertThat(this.calculator.last(plan)).contains(new PlannedDose(oct(25), at(8)));
    }

    @Test
    void everyThreeDaysFollowsTheStartDay() {
        CoursePlan plan = everyDays(3, List.of(at(10)), oct(18), at(10), nov(8), at(10));

        List<LocalDate> days = this.calculator.between(plan, oct(1), nov(30)).stream().map(PlannedDose::on).toList();

        assertThat(days).containsExactly(oct(18), oct(21), oct(24), oct(27), oct(30), nov(2), nov(5));
    }

    @Test
    void aWindowStartingBetweenDoseDaysFindsTheNextOne() {
        CoursePlan plan = everyDays(3, List.of(at(10)), oct(18), at(10), null, null);

        List<LocalDate> days = this.calculator.between(plan, oct(22), oct(28)).stream().map(PlannedDose::on).toList();

        assertThat(days).containsExactly(oct(24), oct(27));
    }

    @Test
    void startingAfterTheLastTimeTodayBeginsTomorrow() {
        CoursePlan plan = daily(List.of(at(8), at(14), at(20)), oct(18), at(21), null, null);

        assertThat(this.calculator.between(plan, oct(18), oct(19)))
                .containsExactly(new PlannedDose(oct(19), at(8)), new PlannedDose(oct(19), at(14)), new PlannedDose(oct(19), at(20)));
    }

    @Test
    void aCourseWithoutAnEndKeepsGoingAndHasNoLastDose() {
        CoursePlan plan = daily(List.of(at(8)), oct(18), at(8), null, null);

        assertThat(this.calculator.between(plan, oct(18), oct(20))).hasSize(3);
        assertThat(this.calculator.last(plan)).isEmpty();
        assertThat(this.calculator.totalDays(plan)).isEmpty();
        assertThat(this.calculator.dayNumber(plan, oct(19))).isEmpty();
    }

    @Test
    void stoppingMidCourseDropsLaterDoses() {
        CoursePlan plan = stoppedAt(daily(List.of(at(8), at(18)), oct(18), at(8), nov(18), at(8)), oct(20), at(12));

        List<PlannedDose> doses = this.calculator.between(plan, oct(1), nov(30));

        assertThat(doses).hasSize(5);
        assertThat(doses.get(4)).isEqualTo(new PlannedDose(oct(20), at(8)));
        assertThat(this.calculator.last(plan)).contains(new PlannedDose(oct(20), at(8)));
    }

    @Test
    void oneDayStartingInTheEveningCoversTheNextMorning() {
        CoursePlan plan = daily(List.of(at(8), at(18)), oct(18), at(18), oct(19), at(18));

        assertThat(this.calculator.between(plan, oct(18), oct(30)))
                .containsExactly(new PlannedDose(oct(18), at(18)), new PlannedDose(oct(19), at(8)));
    }

    @Test
    void theClockChangeDayKeepsTheSameLocalTimes() {
        LocalDate changeDay = LocalDate.of(2026, 10, 25);
        CoursePlan plan = daily(List.of(at(8), at(18)), changeDay.minusDays(1), at(8), changeDay.plusDays(2), at(8));

        assertThat(this.calculator.between(plan, changeDay, changeDay))
                .containsExactly(new PlannedDose(changeDay, at(8)), new PlannedDose(changeDay, at(18)));
    }

    @Test
    void isPlannedMatchesOnlyListedTimesOnDoseDays() {
        CoursePlan plan = everyDays(2, List.of(at(9)), oct(18), at(9), oct(28), at(9));

        assertThat(this.calculator.isPlanned(plan, oct(20), at(9))).isTrue();
        assertThat(this.calculator.isPlanned(plan, oct(19), at(9))).isFalse();
        assertThat(this.calculator.isPlanned(plan, oct(20), at(10))).isFalse();
        assertThat(this.calculator.isPlanned(plan, oct(28), at(9))).isFalse();
        assertThat(this.calculator.isPlanned(plan, oct(16), at(9))).isFalse();
    }

    @Test
    void dayNumbersCountFromTheStartAndStopAtTheEndDay() {
        CoursePlan plan = daily(List.of(at(8), at(18)), oct(18), at(18), oct(25), at(18));

        assertThat(this.calculator.totalDays(plan)).hasValue(7);
        assertThat(this.calculator.dayNumber(plan, oct(18))).hasValue(1);
        assertThat(this.calculator.dayNumber(plan, oct(24))).hasValue(7);
        assertThat(this.calculator.dayNumber(plan, oct(25))).isEmpty();
        assertThat(this.calculator.dayNumber(plan, oct(17))).isEmpty();
    }

    @Test
    void neverReturnsMoreThanTheCap() {
        CoursePlan plan = daily(List.of(at(6), at(8), at(10), at(12), at(14), at(16)), oct(1), 0, null, null);

        assertThat(this.calculator.between(plan, oct(1), LocalDate.of(2027, 12, 31))).hasSize(DoseTimesCalculator.MAX_DOSES);
    }
}
