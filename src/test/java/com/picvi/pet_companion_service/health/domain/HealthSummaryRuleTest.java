package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.health.domain.HealthSummary.SignSummary;
import com.picvi.pet_companion_service.nutrition.api.FoodChangeNow;
import com.picvi.pet_companion_service.nutrition.api.FoodChangeNow.State;
import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.meal;
import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.of;
import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.poo;
import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.symptom;
import static org.assertj.core.api.Assertions.assertThat;

class HealthSummaryRuleTest {
    private static final LocalDate THU = LocalDate.of(2026, 10, 15);
    private static final LocalDate WED = THU.minusDays(1);
    private static final LocalDate TUE = THU.minusDays(2);
    private static final LocalDate MON = THU.minusDays(3);
    private static final LocalDate SUN = THU.minusDays(4);
    private static final LocalDate LONG_AGO = THU.minusDays(60);

    private final HealthSummaryRule rule = new HealthSummaryRule(new WatchEvents());

    @Test
    void aPuppyThatJustArrivedIsSettlingIn() {
        HealthSummary summary = this.summarise(THU.minusDays(1), List.of());

        assertThat(summary.state()).isEqualTo(CardState.SETTLING_IN);
        assertThat(summary.headline()).isNull();
        assertThat(summary.note().code()).isEqualTo(NoteCode.SETTLING_IN);
        assertThat(summary.signs()).extracting(SignSummary::sign).containsExactly(BodySign.EATING, BodySign.POO, BodySign.ENERGY, BodySign.SLEEP);
        assertThat(summary.signs()).extracting(SignSummary::state).containsOnly(SignState.NO_DATA);
    }

    @Test
    void anOlderDogWithNothingLoggedIsQuietNotUsual() {
        HealthSummary summary = this.summarise(LONG_AGO, List.of());

        assertThat(summary.state()).isEqualTo(CardState.QUIET);
        assertThat(summary.note().code()).isEqualTo(NoteCode.QUIET);
    }

    @Test
    void aNewPetWithOldLogsIsNotSettlingIn() {
        HealthSummary summary = this.summarise(THU.minusDays(1), List.of(of(LogType.NOTE, THU.minusDays(10), 600)));

        assertThat(summary.state()).isEqualTo(CardState.QUIET);
    }

    @Test
    void aNormalWeekIsUsual() {
        HealthSummary summary = this.summarise(LONG_AGO, this.normalWeek());

        assertThat(summary.state()).isEqualTo(CardState.USUAL);
        assertThat(summary.note().code()).isEqualTo(NoteCode.NO_SYMPTOMS);
        assertThat(summary.signs()).extracting(SignSummary::state).containsOnly(SignState.USUAL);
    }

    @Test
    void aRunningFoodChangeChangesTheNoteOfAUsualWeek() {
        HealthSummary summary = this.summarise(LONG_AGO, this.normalWeek(), new FoodChangeNow(State.RUNNING, MON));

        assertThat(summary.note().code()).isEqualTo(NoteCode.FOOD_CHANGE);
        assertThat(summary.foodChange().state()).isEqualTo(State.RUNNING);
    }

    @Test
    void diarrhoeaFourTimesSinceSundayIsTheHeadline() {
        HealthSummary summary = this.summarise(LONG_AGO, this.diarrhoea());

        assertThat(summary.state()).isEqualTo(CardState.WATCH);
        assertThat(summary.headline()).satisfies(headline -> {
            assertThat(headline.sign()).isEqualTo(BodySign.POO);
            assertThat(headline.kind()).isEqualTo(WatchKind.LOOSE_STOOL);
            assertThat(headline.count()).isEqualTo(4);
            assertThat(headline.from()).isEqualTo(SUN);
            assertThat(headline.lastOn()).isEqualTo(TUE);
        });
        assertThat(this.sign(summary, BodySign.POO).state()).isEqualTo(SignState.WATCH);
        assertThat(this.sign(summary, BodySign.POO).watch().count()).isEqualTo(4);
        assertThat(summary.note().code()).isEqualTo(NoteCode.CALL_IF_WORSE);
    }

    @Test
    void normalPooOnWednesdayAndThursdayIsGettingBetter() {
        List<LogSnapshot> logs = this.diarrhoea();
        logs.add(poo(WED, 540, 3));
        logs.add(poo(THU, 540, 3));

        HealthSummary summary = this.summarise(LONG_AGO, logs);

        assertThat(summary.state()).isEqualTo(CardState.WATCH);
        assertThat(summary.note().code()).isEqualTo(NoteCode.GETTING_BETTER);
        assertThat(summary.note().sign()).isEqualTo(BodySign.POO);
        assertThat(this.sign(summary, BodySign.POO).watch().normalSince()).isEqualTo(WED);
    }

    @Test
    void oneNormalDayIsNotYetGettingBetter() {
        List<LogSnapshot> logs = this.diarrhoea();
        logs.add(poo(THU, 540, 3));

        assertThat(this.summarise(LONG_AGO, logs).note().code()).isEqualTo(NoteCode.CALL_IF_WORSE);
    }

    @Test
    void aLooseStoolYesterdayIsNotGettingBetter() {
        List<LogSnapshot> logs = this.diarrhoea();
        logs.add(poo(WED, 540, 6));
        logs.add(poo(THU, 540, 3));

        assertThat(this.summarise(LONG_AGO, logs).note().code()).isEqualTo(NoteCode.CALL_IF_WORSE);
    }

    @Test
    void aPausedFoodChangeBeatsGettingBetter() {
        List<LogSnapshot> logs = this.diarrhoea();
        logs.add(poo(WED, 540, 3));
        logs.add(poo(THU, 540, 3));

        HealthSummary summary = this.summarise(LONG_AGO, logs, new FoodChangeNow(State.PAUSED, MON));

        assertThat(summary.note().code()).isEqualTo(NoteCode.FOOD_PAUSED);
    }

    @Test
    void aSymptomFromThirteenDaysAgoKeepsTheCardYellowButNotTheSign() {
        List<LogSnapshot> logs = new ArrayList<>(this.normalWeek());
        logs.add(symptom(THU.minusDays(13), 600, LogDetail.VOMITING));

        HealthSummary summary = this.summarise(LONG_AGO, logs);

        assertThat(summary.state()).isEqualTo(CardState.WATCH);
        assertThat(summary.headline().sign()).isEqualTo(BodySign.VOMIT);
        assertThat(summary.signs()).extracting(SignSummary::sign).containsExactly(BodySign.EATING, BodySign.POO, BodySign.ENERGY, BodySign.SLEEP);
    }

    @Test
    void aSymptomFromFourteenDaysAgoIsForgotten() {
        List<LogSnapshot> logs = new ArrayList<>(this.normalWeek());
        logs.add(symptom(THU.minusDays(14), 600, LogDetail.VOMITING));

        assertThat(this.summarise(LONG_AGO, logs).state()).isEqualTo(CardState.USUAL);
    }

    @Test
    void vomitingTodayDuringAPausedChange() {
        List<LogSnapshot> logs = new ArrayList<>(this.normalWeek());
        logs.add(symptom(THU, 480, LogDetail.VOMITING));

        HealthSummary summary = this.summarise(LONG_AGO, logs, new FoodChangeNow(State.PAUSED, MON));

        assertThat(summary.headline()).satisfies(headline -> {
            assertThat(headline.sign()).isEqualTo(BodySign.VOMIT);
            assertThat(headline.kind()).isEqualTo(WatchKind.SYMPTOM);
            assertThat(headline.detail()).isEqualTo(LogDetail.VOMITING);
            assertThat(headline.count()).isEqualTo(1);
            assertThat(headline.from()).isEqualTo(THU);
            assertThat(headline.lastOn()).isEqualTo(THU);
        });
        assertThat(summary.signs()).extracting(SignSummary::sign)
                .containsExactly(BodySign.EATING, BodySign.POO, BodySign.ENERGY, BodySign.SLEEP, BodySign.VOMIT);
        assertThat(this.sign(summary, BodySign.VOMIT).watch().normalSince()).isNull();
        assertThat(summary.note().code()).isEqualTo(NoteCode.FOOD_PAUSED);
    }

    @Test
    void oneMealLeftIsNotAWatch() {
        List<LogSnapshot> logs = new ArrayList<>(this.normalWeek());
        logs.add(meal(THU, 1080, LogDetail.LEFT_SOME));

        HealthSummary summary = this.summarise(LONG_AGO, logs);

        assertThat(summary.state()).isEqualTo(CardState.USUAL);
        assertThat(this.sign(summary, BodySign.EATING).state()).isEqualTo(SignState.USUAL);
    }

    @Test
    void twoMealsLeftMakeEatingWatchWithANormalMealSince() {
        List<LogSnapshot> logs = new ArrayList<>(this.normalWeek());
        logs.add(meal(MON, 1080, LogDetail.LEFT_SOME));
        logs.add(meal(TUE, 480, LogDetail.LEFT_SOME));

        HealthSummary summary = this.summarise(LONG_AGO, logs);

        assertThat(this.sign(summary, BodySign.EATING).state()).isEqualTo(SignState.WATCH);
        assertThat(this.sign(summary, BodySign.EATING).watch()).satisfies(watch -> {
            assertThat(watch.kind()).isEqualTo(WatchKind.LEFT_FOOD);
            assertThat(watch.count()).isEqualTo(2);
            assertThat(watch.from()).isEqualTo(MON);
            assertThat(watch.to()).isEqualTo(TUE);
            assertThat(watch.lastMinute()).isEqualTo(480);
            assertThat(watch.normalSince()).isEqualTo(WED);
        });
    }

    @Test
    void theHeadlineIsTheMostRecentKind() {
        List<LogSnapshot> logs = new ArrayList<>(this.diarrhoea());
        logs.add(symptom(TUE, 1200, LogDetail.VOMITING));

        HealthSummary summary = this.summarise(LONG_AGO, logs);

        assertThat(summary.headline().sign()).isEqualTo(BodySign.VOMIT);
        assertThat(summary.headline().count()).isEqualTo(1);
    }

    @Test
    void symptomsOnTheSameSignAreCountedPerDetail() {
        List<LogSnapshot> logs = List.of(symptom(MON, 600, LogDetail.SCRATCHING), symptom(TUE, 600, LogDetail.SCRATCHING),
                symptom(WED, 600, LogDetail.SWELLING));

        HealthSummary summary = this.summarise(LONG_AGO, logs);

        assertThat(summary.headline().detail()).isEqualTo(LogDetail.SWELLING);
        assertThat(summary.headline().count()).isEqualTo(1);
        assertThat(this.sign(summary, BodySign.SKIN).watch().detail()).isEqualTo(LogDetail.SWELLING);
    }

    @Test
    void sleepIsUsualOnlyWhenSleepOrWakeWasLogged() {
        HealthSummary withoutSleep = this.summarise(LONG_AGO, List.of(meal(THU, 480, LogDetail.ATE_ALL)));
        HealthSummary withSleep = this.summarise(LONG_AGO, List.of(of(LogType.WAKE, THU, 420)));

        assertThat(this.sign(withoutSleep, BodySign.SLEEP).state()).isEqualTo(SignState.NO_DATA);
        assertThat(this.sign(withoutSleep, BodySign.ENERGY).state()).isEqualTo(SignState.USUAL);
        assertThat(this.sign(withSleep, BodySign.SLEEP).state()).isEqualTo(SignState.USUAL);
        assertThat(this.sign(withSleep, BodySign.EATING).state()).isEqualTo(SignState.NO_DATA);
    }

    @Test
    void aLowEnergySymptomMakesEnergyWatchWithoutAnExtraSign() {
        HealthSummary summary = this.summarise(LONG_AGO, List.of(symptom(THU, 600, LogDetail.LOW_ENERGY)));

        assertThat(this.sign(summary, BodySign.ENERGY).state()).isEqualTo(SignState.WATCH);
        assertThat(summary.signs()).hasSize(4);
    }

    private HealthSummary summarise(LocalDate petAddedOn, List<LogSnapshot> logs) {
        return this.rule.summarise(THU, petAddedOn, logs, Optional.empty());
    }

    private HealthSummary summarise(LocalDate petAddedOn, List<LogSnapshot> logs, FoodChangeNow change) {
        return this.rule.summarise(THU, petAddedOn, logs, Optional.of(change));
    }

    private SignSummary sign(HealthSummary summary, BodySign sign) {
        return summary.signs().stream().filter(candidate -> candidate.sign() == sign).findFirst().orElseThrow();
    }

    private List<LogSnapshot> normalWeek() {
        List<LogSnapshot> logs = new ArrayList<>();
        for (LocalDate day = THU.minusDays(6); !day.isAfter(THU); day = day.plusDays(1)) {
            logs.add(meal(day, 480, LogDetail.ATE_ALL));
            logs.add(poo(day, 540, 3));
            logs.add(of(LogType.SLEEP, day, 800));
        }
        return logs;
    }

    private List<LogSnapshot> diarrhoea() {
        List<LogSnapshot> logs = new ArrayList<>();
        logs.add(poo(SUN, 480, 6));
        logs.add(poo(SUN, 900, 6));
        logs.add(poo(MON, 540, 6));
        logs.add(poo(TUE, 550, 6));
        return logs;
    }
}
