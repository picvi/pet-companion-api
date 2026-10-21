package com.picvi.pet_companion_service.petlog.domain;

import java.util.EnumSet;
import java.util.Set;

import static com.picvi.pet_companion_service.petlog.domain.LogField.*;

public class LogSpecs {

    public LogSpec of(LogType type) {
        return switch (type) {
            case TOILET -> spec(true, Set.of(), ACCIDENT, STOOL_SCORE, FLAGS, NOTE);
            case MEAL -> spec(false, Set.of(), AMOUNT_GRAMS, LABEL, FLAGS, NOTE, FOODS);
            case SLEEP, WAKE -> spec(false, Set.of(), NOTE);
            case WALK -> spec(false, Set.of(DURATION_MINUTES), NOTE);
            case WEIGHT -> spec(false, Set.of(WEIGHT_GRAMS), NOTE);
            case SYMPTOM -> spec(true, Set.of(), WELLBEING, NOTE);
            case MEDICINE -> spec(false, Set.of(LABEL), DOSE, NOTE);
            case NOTE -> spec(false, Set.of(NOTE));
        };
    }

    private static LogSpec spec(boolean detailRequired, Set<LogField> required, LogField... optional) {
        Set<LogField> allowed = EnumSet.noneOf(LogField.class);
        allowed.addAll(required);
        allowed.addAll(Set.of(optional));
        return new LogSpec(detailRequired, Set.copyOf(required), Set.copyOf(allowed));
    }
}
