package com.picvi.pet_companion_service.brief.domain;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.domain.LogDetail;
import com.picvi.pet_companion_service.petlog.domain.LogType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ConcernCounterTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 14);

    private final ConcernCounter counter = new ConcernCounter();

    @Test
    void stoolScoreFourIsNotDiarrhoeaAndFiveIs() {
        Map<Concern, ConcernCounter.Tally> tallies = this.counter.count(List.of(
                log(LogType.TOILET, LogDetail.POO, 4, DAY), log(LogType.TOILET, LogDetail.POO, 5, DAY)));

        assertThat(tallies.get(Concern.DIARRHOEA).count()).isEqualTo(1);
    }

    @Test
    void leftSomeAndRefusedBothCountAsLowAppetite() {
        Map<Concern, ConcernCounter.Tally> tallies = this.counter.count(List.of(
                log(LogType.MEAL, LogDetail.LEFT_SOME, null, DAY), log(LogType.MEAL, LogDetail.REFUSED, null, DAY.plusDays(1)),
                log(LogType.MEAL, LogDetail.ATE_ALL, null, DAY)));

        ConcernCounter.Tally tally = tallies.get(Concern.LOW_APPETITE);
        assertThat(tally.count()).isEqualTo(2);
        assertThat(tally.firstSeen()).isEqualTo(DAY);
        assertThat(tally.lastSeen()).isEqualTo(DAY.plusDays(1));
        assertThat(tally.daily()).containsEntry(DAY, 1).containsEntry(DAY.plusDays(1), 1);
    }

    @Test
    void oneLogCanMatchTwoConcerns() {
        LogSnapshot overlapping = new LogSnapshot(UUID.randomUUID(), LogType.SYMPTOM, LogDetail.VOMITING, 6, null, Set.of(), DAY, 600,
                null, null, null, null);
        LogSnapshot loose = log(LogType.TOILET, LogDetail.POO, 6, DAY);

        Map<Concern, ConcernCounter.Tally> tallies = this.counter.count(List.of(overlapping, loose));

        assertThat(tallies.get(Concern.VOMITING).count()).isEqualTo(1);
        assertThat(tallies.get(Concern.DIARRHOEA).count()).isEqualTo(1);
    }

    @Test
    void concernsWithoutLogsAreAbsentAndOtherNeverAppears() {
        assertThat(this.counter.count(List.of(log(LogType.TOILET, LogDetail.PEE, null, DAY)))).isEmpty();
    }

    private static LogSnapshot log(LogType type, LogDetail detail, Integer stoolScore, LocalDate day) {
        return new LogSnapshot(UUID.randomUUID(), type, detail, stoolScore, null, Set.of(), day, 600, null, null, null, null);
    }
}
