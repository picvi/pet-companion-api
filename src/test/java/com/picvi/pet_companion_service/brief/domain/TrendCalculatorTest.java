package com.picvi.pet_companion_service.brief.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TrendCalculatorTest {
    private static final LocalDate TO = LocalDate.of(2026, 10, 15);

    private final TrendCalculator calculator = new TrendCalculator();

    @Test
    void noneWhenNothingInBothWindows() {
        assertThat(this.calculator.calculate(Map.of(), TO)).isEqualTo(Trend.NONE);
    }

    @Test
    void newWhenOnlyTheRecentWindowHasCounts() {
        assertThat(this.calculator.calculate(Map.of(TO, 1), TO)).isEqualTo(Trend.NEW);
    }

    @Test
    void worseningWhenRecentIsHigher() {
        assertThat(this.calculator.calculate(Map.of(TO, 2, TO.minusDays(4), 1), TO)).isEqualTo(Trend.WORSENING);
    }

    @Test
    void steadyWhenEqual() {
        assertThat(this.calculator.calculate(Map.of(TO, 1, TO.minusDays(4), 1), TO)).isEqualTo(Trend.STEADY);
    }

    @Test
    void improvingWhenRecentIsLower() {
        assertThat(this.calculator.calculate(Map.of(TO, 1, TO.minusDays(4), 3), TO)).isEqualTo(Trend.IMPROVING);
    }

    @Test
    void windowEdgesAreToMinusTwoAndToMinusThree() {
        assertThat(this.calculator.calculate(Map.of(TO.minusDays(2), 1), TO)).isEqualTo(Trend.NEW);
        assertThat(this.calculator.calculate(Map.of(TO.minusDays(3), 1), TO)).isEqualTo(Trend.IMPROVING);
        assertThat(this.calculator.calculate(Map.of(TO.minusDays(5), 1), TO)).isEqualTo(Trend.IMPROVING);
        assertThat(this.calculator.calculate(Map.of(TO.minusDays(6), 1), TO)).isEqualTo(Trend.NONE);
    }
}
