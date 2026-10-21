package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogFlag;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.meal;
import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.of;
import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.poo;
import static com.picvi.pet_companion_service.health.domain.HealthTestLogs.symptom;
import static org.assertj.core.api.Assertions.assertThat;

class WatchEventsTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 15);

    private final WatchEvents watchEvents = new WatchEvents();

    @Test
    void aLooseStoolIsAWatchEvent() {
        List<WatchEvent> events = this.watchEvents.from(List.of(poo(DAY, 540, 5)));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.kind()).isEqualTo(WatchKind.LOOSE_STOOL);
            assertThat(event.sign()).isEqualTo(BodySign.POO);
        });
    }

    @Test
    void aFirmStoolIsNot() {
        assertThat(this.watchEvents.from(List.of(poo(DAY, 540, 4), poo(DAY, 600, 2)))).isEmpty();
    }

    @Test
    void aStoolWithBloodIsAWatchEvenWhenFirm() {
        List<WatchEvent> events = this.watchEvents.from(List.of(poo(DAY, 540, 3, Set.of(LogFlag.BLOOD))));

        assertThat(events).singleElement().extracting(WatchEvent::kind).isEqualTo(WatchKind.STOOL_FLAG);
    }

    @Test
    void aLooseStoolWithBloodIsOneEventAndLooseWins() {
        List<WatchEvent> events = this.watchEvents.from(List.of(poo(DAY, 540, 6, Set.of(LogFlag.BLOOD))));

        assertThat(events).singleElement().extracting(WatchEvent::kind).isEqualTo(WatchKind.LOOSE_STOOL);
    }

    @Test
    void aNewFoodFlagAloneIsNotAWatch() {
        assertThat(this.watchEvents.from(List.of(poo(DAY, 540, 3, Set.of(LogFlag.NEW_FOOD))))).isEmpty();
    }

    @Test
    void oneMealLeftIsNothing() {
        assertThat(this.watchEvents.from(List.of(meal(DAY, 480, LogDetail.LEFT_SOME)))).isEmpty();
    }

    @Test
    void twoMealsLeftAreTwoEvents() {
        List<WatchEvent> events = this.watchEvents.from(List.of(meal(DAY, 480, LogDetail.LEFT_SOME), meal(DAY, 1080, LogDetail.REFUSED)));

        assertThat(events).hasSize(2).allSatisfy(event -> {
            assertThat(event.kind()).isEqualTo(WatchKind.LEFT_FOOD);
            assertThat(event.sign()).isEqualTo(BodySign.EATING);
        });
    }

    @Test
    void aMealEatenIsNothing() {
        assertThat(this.watchEvents.from(List.of(meal(DAY, 480, LogDetail.ATE_ALL), meal(DAY, 1080, LogDetail.ATE_ALL)))).isEmpty();
    }

    @Test
    void anySymptomIsAWatchEventOnItsSign() {
        List<WatchEvent> events = this.watchEvents.from(List.of(symptom(DAY, 600, LogDetail.VOMITING)));

        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.kind()).isEqualTo(WatchKind.SYMPTOM);
            assertThat(event.detail()).isEqualTo(LogDetail.VOMITING);
            assertThat(event.sign()).isEqualTo(BodySign.VOMIT);
        });
    }

    @Test
    void everySymptomDetailHasASign() {
        Arrays.stream(LogDetail.values())
                .filter(detail -> detail.getType() == LogType.SYMPTOM)
                .forEach(detail -> assertThat(SymptomSigns.of(detail)).isNotNull());
    }

    @Test
    void otherLogsAreNotEvents() {
        List<LogSnapshot> logs = List.of(of(LogType.WALK, DAY, 600), of(LogType.SLEEP, DAY, 700), of(LogType.WEIGHT, DAY, 800),
                of(LogType.MEDICINE, DAY, 900), of(LogType.NOTE, DAY, 950));

        assertThat(this.watchEvents.from(logs)).isEmpty();
    }

    @Test
    void eventsComeOldestFirst() {
        List<WatchEvent> events = this.watchEvents.from(List.of(symptom(DAY, 600, LogDetail.VOMITING), poo(DAY.minusDays(1), 900, 6)));

        assertThat(events).extracting(WatchEvent::on).containsExactly(DAY.minusDays(1), DAY);
    }
}
