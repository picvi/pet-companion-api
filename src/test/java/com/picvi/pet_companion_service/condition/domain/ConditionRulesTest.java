package com.picvi.pet_companion_service.condition.domain;

import com.picvi.pet_companion_service.exception.condition.InvalidCondition;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private final ConditionRules rules = new ConditionRules();

    @Test
    void anAllergyMayNameAnIngredient() {
        assertThatCode(() -> this.rules.validate(ConditionKind.ALLERGY, "CHICKEN", null, null, null, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void aConditionNeedsAnIngredientOrWords() {
        assertThatThrownBy(() -> this.rules.validate(ConditionKind.ALLERGY, null, null, null, null, TODAY)).isInstanceOf(InvalidCondition.class);
        assertThatCode(() -> this.rules.validate(ConditionKind.CHRONIC, null, "Epilepsy", null, null, TODAY)).doesNotThrowAnyException();
    }

    @Test
    void aChronicConditionIsNotAnIngredient() {
        assertThatThrownBy(() -> this.rules.validate(ConditionKind.CHRONIC, "BEEF", "x", null, null, TODAY)).isInstanceOf(InvalidCondition.class);
    }

    @Test
    void datesMakeSense() {
        assertThatThrownBy(() -> this.rules.validate(ConditionKind.ALLERGY, "BEEF", null, TODAY.plusDays(1), null, TODAY)).isInstanceOf(InvalidCondition.class);
        assertThatThrownBy(() -> this.rules.validate(ConditionKind.ALLERGY, "BEEF", null, TODAY, TODAY.minusDays(1), TODAY)).isInstanceOf(InvalidCondition.class);
    }

    @Test
    void textHasALimit() {
        assertThatThrownBy(() -> this.rules.validate(ConditionKind.CHRONIC, null, "x".repeat(201), null, null, TODAY)).isInstanceOf(InvalidCondition.class);
    }
}
