package com.picvi.pet_companion_service.nutrition.service;

import com.picvi.pet_companion_service.nutrition.domain.ReviewStatus;
import com.picvi.pet_companion_service.nutrition.dto.FoodSafetyResponse;
import com.picvi.pet_companion_service.nutrition.entity.FoodSafetyItem;
import com.picvi.pet_companion_service.nutrition.repository.FoodSafetyItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FoodSafetyServiceTest {
    @Mock
    private FoodSafetyItemRepository repository;

    private FoodSafetyService service;

    @BeforeEach
    void setUp() {
        this.service = new FoodSafetyService(this.repository);
    }

    @Test
    void oneUnreviewedFoodKeepsTheWholeListPending() {
        when(this.repository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                item("GRAPES", LocalDate.of(2026, 11, 1), "Dr Nowak"), item("CARROT", null, null)));

        FoodSafetyResponse response = this.service.catalogue();

        assertThat(response.review().status()).isEqualTo(ReviewStatus.PENDING);
        assertThat(response.review().reviewedBy()).isNull();
    }

    @Test
    void whenEveryFoodIsReviewedTheNewestReviewIsShown() {
        when(this.repository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                item("GRAPES", LocalDate.of(2026, 11, 1), "Dr Nowak"), item("CARROT", LocalDate.of(2026, 11, 8), "Dr Kowal")));

        FoodSafetyResponse response = this.service.catalogue();

        assertThat(response.review().status()).isEqualTo(ReviewStatus.REVIEWED);
        assertThat(response.review().reviewedBy()).isEqualTo("Dr Kowal");
        assertThat(response.review().reviewedOn()).isEqualTo(LocalDate.of(2026, 11, 8));
    }

    @Test
    void anEmptyListIsPending() {
        when(this.repository.findAllByOrderByCodeAsc()).thenReturn(List.of());

        assertThat(this.service.catalogue().review().status()).isEqualTo(ReviewStatus.PENDING);
    }

    @Test
    void theListIsReadOnceAndKept() {
        when(this.repository.findAllByOrderByCodeAsc()).thenReturn(List.of(item("CARROT", null, null)));

        FoodSafetyResponse first = this.service.catalogue();
        FoodSafetyResponse second = this.service.catalogue();

        assertThat(second).isSameAs(first);
        verify(this.repository, times(1)).findAllByOrderByCodeAsc();
    }

    @Test
    void aFoodWithoutAdviceHasNoHowText() {
        when(this.repository.findAllByOrderByCodeAsc()).thenReturn(List.of(item("CARROT", null, null)));

        assertThat(this.service.catalogue().items().get(0).how()).isNull();
    }

    private static FoodSafetyItem item(String code, LocalDate reviewedOn, String reviewedBy) {
        FoodSafetyItem item = new FoodSafetyItem();
        ReflectionTestUtils.setField(item, "code", code);
        ReflectionTestUtils.setField(item, "nameEn", code);
        ReflectionTestUtils.setField(item, "namePl", code);
        ReflectionTestUtils.setField(item, "whyEn", "why");
        ReflectionTestUtils.setField(item, "whyPl", "dlaczego");
        ReflectionTestUtils.setField(item, "aliasesEn", List.of());
        ReflectionTestUtils.setField(item, "aliasesPl", List.of());
        ReflectionTestUtils.setField(item, "sources", List.of("AKC"));
        ReflectionTestUtils.setField(item, "reviewedOn", reviewedOn);
        ReflectionTestUtils.setField(item, "reviewedBy", reviewedBy);
        return item;
    }
}
