package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PetFoodTest {
    private static final LocalDate MON = LocalDate.of(2026, 10, 19);

    private PetFood foodStartedOn(LocalDate date) {
        return new PetFood(UUID.randomUUID(), UUID.randomUUID(), null, FoodKind.MAIN, 120, false, date,
                UUID.randomUUID(), Instant.now());
    }

    @Test
    void isGivenFromItsStartWhileItHasNoEnd() {
        PetFood food = this.foodStartedOn(MON);

        assertThat(food.isGivenOn(MON.minusDays(1))).isFalse();
        assertThat(food.isGivenOn(MON)).isTrue();
        assertThat(food.isGivenOn(MON.plusDays(100))).isTrue();
    }

    @Test
    void theEndDayIsTheFirstDayItIsNoLongerGiven() {
        PetFood food = this.foodStartedOn(MON);
        food.endOn(MON.plusDays(7));

        assertThat(food.getStatus()).isEqualTo(PetFoodStatus.ENDED);
        assertThat(food.isGivenOn(MON.plusDays(6))).isTrue();
        assertThat(food.isGivenOn(MON.plusDays(7))).isFalse();
    }

    @Test
    void endingOnTheStartDayMeansItWasNeverGiven() {
        PetFood food = this.foodStartedOn(MON);
        food.endOn(MON.minusDays(3));

        assertThat(food.getEndedOn()).isEqualTo(MON);
        assertThat(food.isGivenOn(MON)).isFalse();
    }

    @Test
    void aFoodEnteredInErrorIsNeverGiven() {
        PetFood food = this.foodStartedOn(MON);
        food.markEnteredInError();

        assertThat(food.isGivenOn(MON)).isFalse();
    }
}
