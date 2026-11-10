package com.picvi.pet_companion_service.petlog.domain;

import com.picvi.pet_companion_service.exception.petlog.InvalidLog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PetLogRules {
    private static final Set<LogFlag> STOOL_FLAGS = Set.of(LogFlag.BLOOD, LogFlag.MUCUS, LogFlag.WORMS, LogFlag.BLACK, LogFlag.STRAINING);
    private static final int LABEL_MAX = 80;
    private static final int DOSE_MAX = 40;
    private static final int NOTE_MAX = 500;
    private static final int FOODS_MAX = 6;

    private final LogSpecs specs = new LogSpecs();

    public void validate(LogType type, LogDetail detail, LogValues values) {
        LogSpec spec = this.specs.of(type);
        Set<LogField> present = values.present();

        if (detail == null && spec.detailRequired()) {
            throw new InvalidLog(LogField.DETAIL, InvalidLog.REQUIRED);
        }
        if (detail != null && detail.getType() != type) {
            throw new InvalidLog(LogField.DETAIL, InvalidLog.WRONG_TYPE);
        }
        if (detail != null && detail.isRetired()) {
            throw new InvalidLog(LogField.DETAIL, InvalidLog.RETIRED);
        }
        for (LogField field : present) {
            if (!spec.allowed().contains(field)) {
                throw new InvalidLog(field, InvalidLog.NOT_ALLOWED);
            }
        }
        for (LogField field : spec.required()) {
            if (!present.contains(field)) {
                throw new InvalidLog(field, InvalidLog.REQUIRED);
            }
        }
        this.validateToilet(type, detail, values);
        this.validateFlags(type, values);
        if (type == LogType.SYMPTOM && detail == LogDetail.OTHER && values.note() == null) {
            throw new InvalidLog(LogField.NOTE, InvalidLog.REQUIRED);
        }
        this.validateRange(LogField.STOOL_SCORE, values.stoolScore(), 1, 7);
        this.validateRange(LogField.AMOUNT_GRAMS, values.amountGrams(), 1, 2000);
        this.validateRange(LogField.DURATION_MINUTES, values.durationMinutes(), 1, 600);
        this.validateRange(LogField.WEIGHT_GRAMS, values.weightGrams(), 50, 120000);
        this.validateLength(LogField.LABEL, values.label(), LABEL_MAX);
        this.validateLength(LogField.DOSE, values.dose(), DOSE_MAX);
        this.validateLength(LogField.NOTE, values.note(), NOTE_MAX);
        this.validateFoods(values.foods());
    }

    public void validateCourseDose(LogType type, CourseDose courseDose) {
        if (courseDose != null && type != LogType.MEDICINE) {
            throw new InvalidLog(LogField.COURSE_DOSE, InvalidLog.NOT_ALLOWED);
        }
    }

    private void validateFoods(List<LogFood> foods) {
        if (foods.size() > FOODS_MAX) {
            throw new InvalidLog(LogField.FOODS, InvalidLog.TOO_MANY);
        }
        Set<UUID> seen = new HashSet<>();
        for (LogFood food : foods) {
            if (food.petFoodId() == null) {
                throw new InvalidLog(LogField.FOODS, InvalidLog.REQUIRED);
            }
            if (!seen.add(food.petFoodId())) {
                throw new InvalidLog(LogField.FOODS, InvalidLog.DUPLICATE);
            }
        }
    }

    private void validateToilet(LogType type, LogDetail detail, LogValues values) {
        if (type != LogType.TOILET || detail != LogDetail.PEE) {
            return;
        }
        if (values.stoolScore() != null) {
            throw new InvalidLog(LogField.STOOL_SCORE, InvalidLog.NOT_ALLOWED_FOR_DETAIL);
        }
        if (values.flags().stream().anyMatch(STOOL_FLAGS::contains)) {
            throw new InvalidLog(LogField.FLAGS, InvalidLog.NOT_ALLOWED_FOR_DETAIL);
        }
    }

    private void validateFlags(LogType type, LogValues values) {
        for (LogFlag flag : values.flags()) {
            boolean allowed = flag == LogFlag.NEW_FOOD ? type == LogType.MEAL : type == LogType.TOILET;
            if (!allowed) {
                throw new InvalidLog(LogField.FLAGS, InvalidLog.NOT_ALLOWED);
            }
        }
    }

    private void validateRange(LogField field, Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw new InvalidLog(field, InvalidLog.OUT_OF_RANGE);
        }
    }

    private void validateLength(LogField field, String text, int max) {
        if (text != null && text.length() > max) {
            throw new InvalidLog(field, InvalidLog.TOO_LONG);
        }
    }
}
