package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.breeds.entity.SizeCategory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LifeStageCalculatorTest {
    private static final LocalDate BIRTH_DATE = LocalDate.of(2026, 1, 1);

    private final LifeStageCalculator calculator = new LifeStageCalculator();

    @Test
    void lastDayOfWeekNineIsHomecoming() {
        assertThat(stageAtAgeDays(69, SizeCategory.MEDIUM)).isEqualTo(DevelopmentStage.HOMECOMING);
    }

    @Test
    void weekTenStartsFindingConfidence() {
        assertThat(stageAtAgeDays(70, SizeCategory.MEDIUM)).isEqualTo(DevelopmentStage.FINDING_CONFIDENCE);
    }

    @Test
    void lastDayOfWeekFifteenIsStillFindingConfidence() {
        assertThat(stageAtAgeDays(111, SizeCategory.MEDIUM)).isEqualTo(DevelopmentStage.FINDING_CONFIDENCE);
    }

    @Test
    void weekSixteenStartsTeething() {
        assertThat(stageAtAgeDays(112, SizeCategory.MEDIUM)).isEqualTo(DevelopmentStage.TEETHING);
    }

    @Test
    void weekTwentySixStartsAdolescence() {
        assertThat(stageAtAgeDays(182, SizeCategory.MEDIUM)).isEqualTo(DevelopmentStage.ADOLESCENCE);
    }

    @Test
    void smallDogIsStillAdolescentInWeekFiftyOne() {
        assertThat(stageAtAgeWeeks(51, SizeCategory.SMALL)).isEqualTo(DevelopmentStage.ADOLESCENCE);
    }

    @Test
    void smallDogBecomesYoungAdultInWeekFiftyTwo() {
        assertThat(stageAtAgeWeeks(52, SizeCategory.SMALL)).isEqualTo(DevelopmentStage.YOUNG_ADULT);
    }

    @Test
    void giantDogIsStillAdolescentInWeekFiftyTwo() {
        assertThat(stageAtAgeWeeks(52, SizeCategory.GIANT)).isEqualTo(DevelopmentStage.ADOLESCENCE);
    }

    @Test
    void giantDogBecomesYoungAdultInWeekOneHundredFour() {
        assertThat(stageAtAgeWeeks(104, SizeCategory.GIANT)).isEqualTo(DevelopmentStage.YOUNG_ADULT);
    }

    @Test
    void adolescenceEndsAtSizeSpecificWeek() {
        assertThat(DevelopmentStage.ADOLESCENCE.endWeek(SizeCategory.SMALL)).isEqualTo(52);
        assertThat(DevelopmentStage.ADOLESCENCE.endWeek(SizeCategory.MEDIUM)).isEqualTo(65);
        assertThat(DevelopmentStage.ADOLESCENCE.endWeek(SizeCategory.LARGE)).isEqualTo(78);
        assertThat(DevelopmentStage.ADOLESCENCE.endWeek(SizeCategory.GIANT)).isEqualTo(104);
    }

    @Test
    void youngAdultHasNoEndAndNoNextStage() {
        StageInfo info = infoAtAgeWeeks(200, SizeCategory.LARGE);

        assertThat(info.stage()).isEqualTo(DevelopmentStage.YOUNG_ADULT);
        assertThat(info.stageEndWeek()).isNull();
        assertThat(info.weeksLeftInStage()).isNull();
        assertThat(info.nextStage()).isNull();
        assertThat(info.nextStageStartWeek()).isNull();
    }

    @Test
    void weeksLeftInWindowAtElevenWeeksIsFive() {
        assertThat(infoAtAgeWeeks(11, SizeCategory.MEDIUM).weeksLeftInWindow()).isEqualTo(5);
    }

    @Test
    void weeksLeftInWindowIsNullAfterTheWindow() {
        assertThat(infoAtAgeWeeks(20, SizeCategory.MEDIUM).weeksLeftInWindow()).isNull();
    }

    @Test
    void daysInStageIsZeroOnFirstDayOfStage() {
        assertThat(infoAtAgeDays(70, SizeCategory.MEDIUM).daysInStage()).isZero();
        assertThat(infoAtAgeDays(71, SizeCategory.MEDIUM).daysInStage()).isEqualTo(1);
    }

    @Test
    void stageInfoDescribesPositionInStage() {
        StageInfo info = infoAtAgeDays(80, SizeCategory.MEDIUM);

        assertThat(info.ageDays()).isEqualTo(80);
        assertThat(info.ageWeeks()).isEqualTo(11);
        assertThat(info.stageStartWeek()).isEqualTo(10);
        assertThat(info.stageEndWeek()).isEqualTo(16);
        assertThat(info.daysInStage()).isEqualTo(10);
        assertThat(info.weeksLeftInStage()).isEqualTo(5);
        assertThat(info.nextStage()).isEqualTo(DevelopmentStage.TEETHING);
        assertThat(info.nextStageStartWeek()).isEqualTo(16);
    }

    @Test
    void dateBeforeBirthDateIsRejected() {
        assertThatThrownBy(() -> calculator.calculate(BIRTH_DATE, SizeCategory.SMALL, BIRTH_DATE.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DevelopmentStage stageAtAgeDays(int ageDays, SizeCategory size) {
        return infoAtAgeDays(ageDays, size).stage();
    }

    private DevelopmentStage stageAtAgeWeeks(int ageWeeks, SizeCategory size) {
        return infoAtAgeWeeks(ageWeeks, size).stage();
    }

    private StageInfo infoAtAgeWeeks(int ageWeeks, SizeCategory size) {
        return infoAtAgeDays(ageWeeks * 7, size);
    }

    private StageInfo infoAtAgeDays(int ageDays, SizeCategory size) {
        return calculator.calculate(BIRTH_DATE, size, BIRTH_DATE.plusDays(ageDays));
    }
}
