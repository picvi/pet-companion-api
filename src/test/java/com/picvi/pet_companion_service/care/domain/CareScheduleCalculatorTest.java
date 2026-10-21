package com.picvi.pet_companion_service.care.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.picvi.pet_companion_service.care.CareCatalogueFixture.DEWORMING;
import static com.picvi.pet_companion_service.care.CareCatalogueFixture.DHPPI;
import static com.picvi.pet_companion_service.care.CareCatalogueFixture.KENNEL_COUGH;
import static com.picvi.pet_companion_service.care.CareCatalogueFixture.RABIES;
import static com.picvi.pet_companion_service.care.CareCatalogueFixture.TICK_PROTECTION;
import static org.assertj.core.api.Assertions.assertThat;

class CareScheduleCalculatorTest {
    private static final LocalDate BIRTH = LocalDate.of(2026, 6, 17);
    private static final CareItemSettings ON = new CareItemSettings(true, null, null);

    private final CareScheduleCalculator calculator = new CareScheduleCalculator();

    @Test
    void rabiesInsideTheLegalWindowIsDueWithTheDeadline() {
        CareItemSchedule schedule = calculate(RABIES, List.of(), ON, LocalDate.of(2026, 10, 14));

        assertThat(schedule.status()).isEqualTo(CareStatus.DUE);
        assertThat(schedule.availableFrom()).isEqualTo(LocalDate.of(2026, 9, 17));
        assertThat(schedule.legalDeadline()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(schedule.dueOn()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(schedule.remindOn()).containsExactly(LocalDate.of(2026, 10, 17));
    }

    @Test
    void rabiesIsOverdueTheDayAfterTheDeadline() {
        assertThat(calculate(RABIES, List.of(), ON, LocalDate.of(2026, 10, 17)).status()).isEqualTo(CareStatus.DUE);
        assertThat(calculate(RABIES, List.of(), ON, LocalDate.of(2026, 10, 18)).status()).isEqualTo(CareStatus.OVERDUE);
    }

    @Test
    void rabiesBeforeThreeMonthsIsNotYetDueUntilAWeekBefore() {
        assertThat(calculate(RABIES, List.of(), ON, LocalDate.of(2026, 9, 9)).status()).isEqualTo(CareStatus.NOT_YET_DUE);
        assertThat(calculate(RABIES, List.of(), ON, LocalDate.of(2026, 9, 10)).status()).isEqualTo(CareStatus.DUE_SOON);
    }

    @Test
    void rabiesRemindsOnTheFirstAvailableDayAWeekBeforeAndOnTheDeadline() {
        CareItemSchedule schedule = calculate(RABIES, List.of(), ON, LocalDate.of(2026, 9, 1));

        assertThat(schedule.remindOn()).containsExactly(
                LocalDate.of(2026, 9, 17), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 17));
    }

    @Test
    void adultNeverVaccinatedAgainstRabiesIsOverdue() {
        CareItemSchedule schedule = calculate(RABIES, List.of(), ON, LocalDate.of(2027, 6, 17));

        assertThat(schedule.status()).isEqualTo(CareStatus.OVERDUE);
    }

    @Test
    void rabiesAfterADoseIsDueTwelveMonthsLaterWithoutLegalDeadline() {
        LocalDate dose = LocalDate.of(2026, 10, 14);

        CareItemSchedule schedule = calculate(RABIES, List.of(dose), ON, dose);

        assertThat(schedule.status()).isEqualTo(CareStatus.UP_TO_DATE);
        assertThat(schedule.dueOn()).isEqualTo(LocalDate.of(2027, 10, 14));
        assertThat(schedule.legalDeadline()).isNull();
        assertThat(schedule.availableFrom()).isNull();
        assertThat(schedule.phase()).isEqualTo(CarePhase.REPEAT);
    }

    @Test
    void rabiesIsOverdueWhenTheYearRunsOut() {
        LocalDate dose = LocalDate.of(2026, 10, 14);

        assertThat(calculate(RABIES, List.of(dose), ON, LocalDate.of(2027, 10, 14)).status()).isEqualTo(CareStatus.DUE);
        assertThat(calculate(RABIES, List.of(dose), ON, LocalDate.of(2027, 10, 15)).status()).isEqualTo(CareStatus.OVERDUE);
        assertThat(calculate(RABIES, List.of(dose), ON, LocalDate.of(2027, 10, 7)).status()).isEqualTo(CareStatus.DUE_SOON);
    }

    @Test
    void puppyWithNothingLoggedIsDueTheFirstSeriesDose() {
        CareItemSchedule schedule = calculate(DHPPI, List.of(), ON, BIRTH.plusWeeks(8));

        assertThat(schedule.status()).isEqualTo(CareStatus.DUE);
        assertThat(schedule.phase()).isEqualTo(CarePhase.SERIES);
        assertThat(schedule.doseNumber()).isEqualTo(1);
        assertThat(schedule.seriesTotal()).isEqualTo(3);
        assertThat(schedule.dueOn()).isEqualTo(BIRTH.plusWeeks(6));
    }

    @Test
    void secondSeriesDoseWaitsForTheLaterOfTheGapAndTheMinimumAge() {
        LocalDate first = BIRTH.plusWeeks(6);

        CareItemSchedule schedule = calculate(DHPPI, List.of(first), ON, first);

        assertThat(schedule.doseNumber()).isEqualTo(2);
        assertThat(schedule.dueOn()).isEqualTo(BIRTH.plusWeeks(9));
        assertThat(schedule.status()).isEqualTo(CareStatus.UP_TO_DATE);
    }

    @Test
    void lateFirstDosePushesTheNextOneByTheGap() {
        LocalDate first = BIRTH.plusWeeks(10);

        CareItemSchedule schedule = calculate(DHPPI, List.of(first), ON, first);

        assertThat(schedule.dueOn()).isEqualTo(first.plusDays(21));
    }

    @Test
    void afterTheSeriesComesTheBoosterThenTheLongRepeat() {
        LocalDate third = BIRTH.plusWeeks(16);
        List<LocalDate> series = List.of(BIRTH.plusWeeks(6), BIRTH.plusWeeks(9), third);

        CareItemSchedule afterSeries = calculate(DHPPI, series, ON, third);
        assertThat(afterSeries.phase()).isEqualTo(CarePhase.BOOSTER);
        assertThat(afterSeries.doseNumber()).isNull();
        assertThat(afterSeries.dueOn()).isEqualTo(third.plusDays(365));

        LocalDate booster = third.plusDays(365);
        CareItemSchedule afterBooster = calculate(DHPPI, List.of(series.get(0), series.get(1), third, booster), ON, booster);
        assertThat(afterBooster.phase()).isEqualTo(CarePhase.REPEAT);
        assertThat(afterBooster.dueOn()).isEqualTo(booster.plusDays(1095));
    }

    @Test
    void twoEventsOnTheSameDayCountAsOneDose() {
        LocalDate first = BIRTH.plusWeeks(6);

        CareItemSchedule schedule = calculate(DHPPI, List.of(first, first), ON, first);

        assertThat(schedule.doseNumber()).isEqualTo(2);
    }

    @Test
    void adoptedAdultWithAnOldDoseIsOverdue() {
        LocalDate old = BIRTH.plusWeeks(8);

        CareItemSchedule schedule = calculate(DHPPI, List.of(old), ON, LocalDate.of(2027, 3, 1));

        assertThat(schedule.status()).isEqualTo(CareStatus.OVERDUE);
        assertThat(schedule.lastPerformedOn()).isEqualTo(old);
    }

    @Test
    void tickProtectionUsesTheDefaultAndTheCustomInterval() {
        LocalDate last = LocalDate.of(2026, 10, 1);
        LocalDate today = LocalDate.of(2026, 10, 14);

        CareItemSchedule byDefault = calculate(TICK_PROTECTION, List.of(last), ON, today);
        assertThat(byDefault.dueOn()).isEqualTo(last.plusDays(28));
        assertThat(byDefault.defaultIntervalDays()).isEqualTo(28);
        assertThat(byDefault.customIntervalDays()).isNull();

        CareItemSchedule custom = calculate(TICK_PROTECTION, List.of(last), new CareItemSettings(true, null, 56), today);
        assertThat(custom.dueOn()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(custom.customIntervalDays()).isEqualTo(56);
        assertThat(custom.remindOn()).containsExactly(LocalDate.of(2026, 11, 19), LocalDate.of(2026, 11, 26));
    }

    @Test
    void resettingTheIntervalReturnsToTheRecommendedSchedule() {
        LocalDate last = LocalDate.of(2026, 10, 1);
        LocalDate today = LocalDate.of(2026, 10, 14);

        CareItemSchedule reset = calculate(TICK_PROTECTION, List.of(last), new CareItemSettings(true, null, null), today);

        assertThat(reset.dueOn()).isEqualTo(last.plusDays(28));
    }

    @Test
    void tickProtectionIsPausedOutsideTheSeasonAndResumesInMarch() {
        LocalDate last = LocalDate.of(2026, 11, 20);

        CareItemSchedule winter = calculate(TICK_PROTECTION, List.of(last), ON, LocalDate.of(2026, 12, 20));

        assertThat(winter.status()).isEqualTo(CareStatus.PAUSED);
        assertThat(winter.dueOn()).isEqualTo(LocalDate.of(2027, 3, 1));
        assertThat(winter.remindOn()).isEmpty();

        CareItemSchedule spring = calculate(TICK_PROTECTION, List.of(last), ON, LocalDate.of(2027, 3, 1));
        assertThat(spring.status()).isEqualTo(CareStatus.DUE);
    }

    @Test
    void puppyKeepsMonthlyDewormingEvenWithACustomInterval() {
        LocalDate last = BIRTH.plusWeeks(10);
        CareItemSettings custom = new CareItemSettings(true, null, 90);

        CareItemSchedule schedule = calculate(DEWORMING, List.of(last), custom, last);

        assertThat(schedule.dueOn()).isEqualTo(last.plusDays(30));
        assertThat(schedule.defaultIntervalDays()).isEqualTo(90);
        assertThat(schedule.customIntervalDays()).isEqualTo(90);
    }

    @Test
    void olderDogFollowsTheCustomDewormingInterval() {
        LocalDate last = BIRTH.plusWeeks(30);
        LocalDate today = last.plusDays(10);

        assertThat(calculate(DEWORMING, List.of(last), ON, today).dueOn()).isEqualTo(last.plusDays(90));
        assertThat(calculate(DEWORMING, List.of(last), new CareItemSettings(true, null, 60), today).dueOn())
                .isEqualTo(last.plusDays(60));
    }

    @Test
    void shorteningTheIntervalCanMakeTheItemOverdueAtOnce() {
        LocalDate last = BIRTH.plusWeeks(30);
        LocalDate today = last.plusDays(40);

        CareItemSchedule schedule = calculate(DEWORMING, List.of(last), new CareItemSettings(true, null, 28), today);

        assertThat(schedule.status()).isEqualTo(CareStatus.OVERDUE);
    }

    @Test
    void neverDewormedPuppyIsDueFromTwoWeeks() {
        CareItemSchedule schedule = calculate(DEWORMING, List.of(), ON, BIRTH.plusWeeks(6));

        assertThat(schedule.status()).isEqualTo(CareStatus.DUE);
        assertThat(schedule.dueOn()).isEqualTo(BIRTH.plusWeeks(2));
    }

    @Test
    void vaccinesIgnoreACustomInterval() {
        LocalDate dose = LocalDate.of(2026, 10, 14);

        CareItemSchedule schedule = calculate(RABIES, List.of(dose), new CareItemSettings(true, null, 30), dose);

        assertThat(schedule.dueOn()).isEqualTo(LocalDate.of(2027, 10, 14));
        assertThat(schedule.intervalEditable()).isFalse();
        assertThat(schedule.customIntervalDays()).isNull();
        assertThat(schedule.defaultIntervalDays()).isNull();
    }

    @Test
    void pausedItemHasNoRemindersUntilItResumes() {
        LocalDate last = LocalDate.of(2026, 10, 1);
        CareItemSettings paused = new CareItemSettings(true, LocalDate.of(2026, 12, 1), null);

        CareItemSchedule schedule = calculate(TICK_PROTECTION, List.of(last), paused, LocalDate.of(2026, 10, 14));

        assertThat(schedule.status()).isEqualTo(CareStatus.PAUSED);
        assertThat(schedule.remindOn()).isEmpty();
        assertThat(schedule.pausedUntil()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    void itemWithRemindersOffHasNoReminderDates() {
        CareItemSchedule schedule = calculate(RABIES, List.of(), new CareItemSettings(false, null, null), LocalDate.of(2026, 10, 14));

        assertThat(schedule.remindOn()).isEmpty();
        assertThat(schedule.remindersEnabled()).isFalse();
    }

    @Test
    void optionalItemsStartWithRemindersOff() {
        assertThat(CareItemSettings.defaultsFor(CareRequirement.OPTIONAL).remindersEnabled()).isFalse();
        assertThat(CareItemSettings.defaultsFor(CareRequirement.REQUIRED).remindersEnabled()).isTrue();
        assertThat(CareItemSettings.defaultsFor(CareRequirement.RECOMMENDED).remindersEnabled()).isTrue();
    }

    @Test
    void itemsAreOrderedRequiredFirstThenByUrgency() {
        LocalDate today = LocalDate.of(2026, 10, 14);
        CareItemSchedule rabies = calculate(RABIES, List.of(LocalDate.of(2026, 10, 1)), ON, today);
        CareItemSchedule dhppiOverdue = calculate(DHPPI, List.of(BIRTH.plusWeeks(6)), ON, today);
        CareItemSchedule kennelDue = calculate(KENNEL_COUGH, List.of(), ON, today);

        List<CareItemSchedule> ordered = calculator.order(List.of(kennelDue, dhppiOverdue, rabies));

        assertThat(ordered).extracting(CareItemSchedule::code).containsExactly("RABIES", "DHPPI", "KENNEL_COUGH");
    }

    @Test
    void alertIsTheMostUrgentItemWithRemindersOn() {
        LocalDate today = LocalDate.of(2026, 10, 14);
        CareItemSchedule rabiesDue = calculate(RABIES, List.of(), ON, today);
        CareItemSchedule dhppiOverdue = calculate(DHPPI, List.of(BIRTH.plusWeeks(6)), ON, today);

        assertThat(calculator.alertCode(List.of(rabiesDue, dhppiOverdue))).contains("DHPPI_OVERDUE");
    }

    @Test
    void alertIgnoresItemsWithRemindersOffAndIsEmptyWhenAllIsWell() {
        LocalDate today = LocalDate.of(2026, 10, 14);
        CareItemSchedule muted = calculate(RABIES, List.of(), new CareItemSettings(false, null, null), today);
        CareItemSchedule fine = calculate(RABIES, List.of(today), ON, today);

        assertThat(calculator.alertCode(List.of(muted))).isEmpty();
        assertThat(calculator.alertCode(List.of(fine))).isEmpty();
    }

    private CareItemSchedule calculate(CareItemDefinition item, List<LocalDate> events, CareItemSettings settings, LocalDate today) {
        return calculator.calculate(item, BIRTH, events, settings, today);
    }
}
