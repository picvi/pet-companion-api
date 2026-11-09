package com.picvi.pet_companion_service.medical.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisitHistoryPagingTest {
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

    private final VisitHistoryPaging paging = new VisitHistoryPaging();

    @Test
    void theSizeDefaultsAndStaysInRange() {
        assertThat(this.paging.size(null)).isEqualTo(20);
        assertThat(this.paging.size(0)).isEqualTo(1);
        assertThat(this.paging.size(500)).isEqualTo(50);
        assertThat(this.paging.size(8)).isEqualTo(8);
    }

    @Test
    void aShortListIsOnePageWithNoNextPage() {
        VisitHistoryPaging.Page<LocalDate> page = this.paging.cut(List.of(DAY, DAY.minusDays(1)), 3, day -> day);

        assertThat(page.rows()).containsExactly(DAY, DAY.minusDays(1));
        assertThat(page.nextBefore()).isNull();
    }

    @Test
    void theNextPageStartsAfterTheLastWholeDay() {
        List<LocalDate> rows = List.of(DAY, DAY.minusDays(1), DAY.minusDays(2), DAY.minusDays(3));

        VisitHistoryPaging.Page<LocalDate> page = this.paging.cut(rows, 3, day -> day);

        assertThat(page.rows()).containsExactly(DAY, DAY.minusDays(1), DAY.minusDays(2));
        assertThat(page.nextBefore()).isEqualTo(DAY.minusDays(2));
    }

    @Test
    void aDayThatWouldBeSplitMovesToTheNextPage() {
        List<LocalDate> rows = List.of(DAY, DAY.minusDays(1), DAY.minusDays(1), DAY.minusDays(1));

        VisitHistoryPaging.Page<LocalDate> page = this.paging.cut(rows, 3, day -> day);

        assertThat(page.rows()).containsExactly(DAY);
        assertThat(page.nextBefore()).isEqualTo(DAY);
    }
}
