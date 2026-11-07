package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidLog;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PetLogRulesTest {
    private final PetLogRules rules = new PetLogRules();

    @Test
    void toiletPooWithStoolScoreAndFlagIsValid() {
        this.assertValid(LogType.TOILET, LogDetail.POO, values(null, 5, Set.of(LogFlag.MUCUS), null, null, null, null, null, null, null));
    }

    @Test
    void toiletNeedsDetail() {
        this.assertInvalid(LogType.TOILET, null, LogValues.none(), LogField.DETAIL, InvalidLog.REQUIRED);
    }

    @Test
    void peeCannotHaveStoolScore() {
        this.assertInvalid(LogType.TOILET, LogDetail.PEE, values(null, 3, null, null, null, null, null, null, null, null),
                LogField.STOOL_SCORE, InvalidLog.NOT_ALLOWED_FOR_DETAIL);
    }

    @Test
    void peeCannotHaveStoolFlag() {
        this.assertInvalid(LogType.TOILET, LogDetail.PEE, values(null, null, Set.of(LogFlag.BLOOD), null, null, null, null, null, null, null),
                LogField.FLAGS, InvalidLog.NOT_ALLOWED_FOR_DETAIL);
    }

    @Test
    void stoolScoreAboveSevenIsOutOfRange() {
        this.assertInvalid(LogType.TOILET, LogDetail.POO, values(null, 8, null, null, null, null, null, null, null, null),
                LogField.STOOL_SCORE, InvalidLog.OUT_OF_RANGE);
    }

    @Test
    void peeMayBeAnAccident() {
        this.assertValid(LogType.TOILET, LogDetail.PEE, values(true, null, null, null, null, null, null, null, null, null));
    }

    @Test
    void retiredAccidentDetailIsRejected() {
        this.assertInvalid(LogType.TOILET, LogDetail.ACCIDENT, LogValues.none(), LogField.DETAIL, InvalidLog.RETIRED);
    }

    @Test
    void detailOfAnotherTypeIsRejected() {
        this.assertInvalid(LogType.TOILET, LogDetail.ATE_ALL, LogValues.none(), LogField.DETAIL, InvalidLog.WRONG_TYPE);
    }

    @Test
    void mealWithNothingIsValid() {
        this.assertValid(LogType.MEAL, null, LogValues.none());
    }

    @Test
    void mealHasNoAmount() {
        this.assertInvalid(LogType.MEAL, LogDetail.ATE_ALL, values(null, null, null, 40, null, null, null, null, null, null),
                LogField.AMOUNT_GRAMS, InvalidLog.NOT_ALLOWED);
    }

    @Test
    void mealMayHaveNewFoodFlagAndLabel() {
        this.assertValid(LogType.MEAL, LogDetail.ATE_ALL, values(null, null, Set.of(LogFlag.NEW_FOOD), null, null, null, null, "Kibble", null, null));
    }

    @Test
    void newFoodFlagIsNotAllowedOnToilet() {
        this.assertInvalid(LogType.TOILET, LogDetail.POO, values(null, null, Set.of(LogFlag.NEW_FOOD), null, null, null, null, null, null, null),
                LogField.FLAGS, InvalidLog.NOT_ALLOWED);
    }

    @Test
    void stoolFlagIsNotAllowedOnMeal() {
        this.assertInvalid(LogType.MEAL, null, values(null, null, Set.of(LogFlag.BLOOD), null, null, null, null, null, null, null),
                LogField.FLAGS, InvalidLog.NOT_ALLOWED);
    }

    @Test
    void mealCannotBeAnAccident() {
        this.assertInvalid(LogType.MEAL, null, values(true, null, null, null, null, null, null, null, null, null),
                LogField.ACCIDENT, InvalidLog.NOT_ALLOWED);
    }

    @Test
    void sleepMayHaveANote() {
        this.assertValid(LogType.SLEEP, null, values(null, null, null, null, null, null, null, null, null, "Slept in the crate"));
    }

    @Test
    void sleepCannotHaveAnAmount() {
        this.assertInvalid(LogType.SLEEP, null, values(null, null, null, 10, null, null, null, null, null, null),
                LogField.AMOUNT_GRAMS, InvalidLog.NOT_ALLOWED);
    }

    @Test
    void walkNeedsDuration() {
        this.assertInvalid(LogType.WALK, null, LogValues.none(), LogField.DURATION_MINUTES, InvalidLog.REQUIRED);
    }

    @Test
    void walkLongerThanSixHundredMinutesIsOutOfRange() {
        this.assertInvalid(LogType.WALK, null, values(null, null, null, null, null, 601, null, null, null, null),
                LogField.DURATION_MINUTES, InvalidLog.OUT_OF_RANGE);
    }

    @Test
    void weightNeedsWeightGrams() {
        this.assertInvalid(LogType.WEIGHT, null, LogValues.none(), LogField.WEIGHT_GRAMS, InvalidLog.REQUIRED);
    }

    @Test
    void weightBelowFiftyGramsIsOutOfRange() {
        this.assertInvalid(LogType.WEIGHT, null, values(null, null, null, null, 49, null, null, null, null, null),
                LogField.WEIGHT_GRAMS, InvalidLog.OUT_OF_RANGE);
    }

    @Test
    void weightOfAPuppyIsValid() {
        this.assertValid(LogType.WEIGHT, null, values(null, null, null, null, 2300, null, null, null, null, null));
    }

    @Test
    void symptomNeedsDetail() {
        this.assertInvalid(LogType.SYMPTOM, null, LogValues.none(), LogField.DETAIL, InvalidLog.REQUIRED);
    }

    @Test
    void symptomMayHaveWellbeing() {
        this.assertValid(LogType.SYMPTOM, LogDetail.VOMITING, values(null, null, null, null, null, null, Wellbeing.QUIETER, null, null, null));
    }

    @Test
    void otherSymptomNeedsANote() {
        this.assertInvalid(LogType.SYMPTOM, LogDetail.OTHER, LogValues.none(), LogField.NOTE, InvalidLog.REQUIRED);
    }

    @Test
    void otherSymptomWithBlankNoteNeedsANote() {
        this.assertInvalid(LogType.SYMPTOM, LogDetail.OTHER, values(null, null, null, null, null, null, null, null, null, "  "),
                LogField.NOTE, InvalidLog.REQUIRED);
    }

    @Test
    void medicineNeedsALabel() {
        this.assertInvalid(LogType.MEDICINE, null, LogValues.none(), LogField.LABEL, InvalidLog.REQUIRED);
    }

    @Test
    void medicineWithLabelAndDoseIsValid() {
        this.assertValid(LogType.MEDICINE, null, values(null, null, null, null, null, null, null, "Enteroferment", "1 sachet", null));
    }

    @Test
    void doseOfFortyOneCharactersIsTooLong() {
        this.assertInvalid(LogType.MEDICINE, null, values(null, null, null, null, null, null, null, "Drug", "x".repeat(41), null),
                LogField.DOSE, InvalidLog.TOO_LONG);
    }

    @Test
    void noteTypeNeedsANote() {
        this.assertInvalid(LogType.NOTE, null, LogValues.none(), LogField.NOTE, InvalidLog.REQUIRED);
    }

    @Test
    void noteOfFiveHundredAndOneCharactersIsTooLong() {
        this.assertInvalid(LogType.NOTE, null, values(null, null, null, null, null, null, null, null, null, "x".repeat(501)),
                LogField.NOTE, InvalidLog.TOO_LONG);
    }

    private void assertValid(LogType type, LogDetail detail, LogValues values) {
        assertThatCode(() -> this.rules.validate(type, detail, values)).doesNotThrowAnyException();
    }

    private void assertInvalid(LogType type, LogDetail detail, LogValues values, LogField field, String reason) {
        assertThatThrownBy(() -> this.rules.validate(type, detail, values))
                .isInstanceOfSatisfying(InvalidLog.class, ex -> {
                    assertThat(ex.getField()).isEqualTo(field);
                    assertThat(ex.getReason()).isEqualTo(reason);
                });
    }

    private static LogValues values(Boolean accident, Integer stoolScore, Set<LogFlag> flags, Integer amountGrams,
                                    Integer weightGrams, Integer durationMinutes, Wellbeing wellbeing, String label,
                                    String dose, String note) {
        return new LogValues(accident, stoolScore, flags, amountGrams, weightGrams, durationMinutes, wellbeing, label, dose, note);
    }

    @Test
    void mealMayLinkSeveralOfThePetsFoods() {
        LogValues values = new LogValues(null, null, null, null, null, null, null, null, null, null,
                List.of(new LogFood(UUID.randomUUID()), new LogFood(UUID.randomUUID())));

        assertThatCode(() -> this.rules.validate(LogType.MEAL, LogDetail.ATE_ALL, values)).doesNotThrowAnyException();
    }

    @Test
    void foodsAreNotAllowedOnAToilet() {
        LogValues values = new LogValues(null, null, null, null, null, null, null, null, null, null,
                List.of(new LogFood(UUID.randomUUID())));

        assertThatThrownBy(() -> this.rules.validate(LogType.TOILET, LogDetail.PEE, values))
                .isInstanceOfSatisfying(InvalidLog.class, e -> assertThat(e.getField()).isEqualTo(LogField.FOODS));
    }

    @Test
    void theSameFoodTwiceIsRejected() {
        UUID food = UUID.randomUUID();
        LogValues values = new LogValues(null, null, null, null, null, null, null, null, null, null,
                List.of(new LogFood(food), new LogFood(food)));

        assertThatThrownBy(() -> this.rules.validate(LogType.MEAL, null, values))
                .isInstanceOfSatisfying(InvalidLog.class, e -> assertThat(e.getReason()).isEqualTo(InvalidLog.DUPLICATE));
    }

    @Test
    void tooManyFoodsAreRejected() {
        List<LogFood> seven = java.util.stream.IntStream.range(0, 7).mapToObj(i -> new LogFood(UUID.randomUUID())).toList();
        LogValues values = new LogValues(null, null, null, null, null, null, null, null, null, null, seven);

        assertThatThrownBy(() -> this.rules.validate(LogType.MEAL, null, values))
                .isInstanceOfSatisfying(InvalidLog.class, e -> assertThat(e.getReason()).isEqualTo(InvalidLog.TOO_MANY));
    }
}
