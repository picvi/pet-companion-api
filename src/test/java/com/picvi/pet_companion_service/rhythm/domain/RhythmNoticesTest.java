package com.picvi.pet_companion_service.rhythm.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.picvi.pet_companion_service.rhythm.RhythmRuleFixture.ELEVEN_WEEKS;
import static org.assertj.core.api.Assertions.assertThat;

class RhythmNoticesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    private final RhythmNotices notices = new RhythmNotices();

    @Test
    void thereIsNoBandNoteInTheBandWhereTheRhythmWasSetUp() {
        assertThat(this.notices.bandNoteFromWeeks(10, ELEVEN_WEEKS)).isNull();
    }

    @Test
    void thereIsABandNoteOnceThePuppyEntersAnotherBand() {
        assertThat(this.notices.bandNoteFromWeeks(8, ELEVEN_WEEKS)).isEqualTo(10);
    }

    @Test
    void thereIsNoBandNoteWithoutSettings() {
        assertThat(this.notices.bandNoteFromWeeks(null, ELEVEN_WEEKS)).isNull();
    }

    @Test
    void theCheckinComesTwoWeeksAfterSetupUntilItIsAnswered() {
        assertThat(this.notices.checkinDue(TODAY.minusDays(13), null, TODAY)).isFalse();
        assertThat(this.notices.checkinDue(TODAY.minusDays(14), null, TODAY)).isTrue();
        assertThat(this.notices.checkinDue(TODAY.minusDays(40), TODAY.minusDays(2), TODAY)).isFalse();
    }
}
