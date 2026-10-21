package com.picvi.pet_companion_service.petlog.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record LogValues(Boolean accident,
                        Integer stoolScore,
                        Set<LogFlag> flags,
                        Integer amountGrams,
                        Integer weightGrams,
                        Integer durationMinutes,
                        Wellbeing wellbeing,
                        String label,
                        String dose,
                        String note,
                        List<LogFood> foods) {

    public LogValues {
        flags = flags == null ? Set.of() : Set.copyOf(flags);
        label = trimmedOrNull(label);
        dose = trimmedOrNull(dose);
        note = trimmedOrNull(note);
        foods = foods == null ? List.of() : List.copyOf(foods);
    }

    public LogValues(Boolean accident, Integer stoolScore, Set<LogFlag> flags, Integer amountGrams, Integer weightGrams,
                     Integer durationMinutes, Wellbeing wellbeing, String label, String dose, String note) {
        this(accident, stoolScore, flags, amountGrams, weightGrams, durationMinutes, wellbeing, label, dose, note, List.of());
    }

    /** The same values with one more flag. */
    public LogValues withFlag(LogFlag flag) {
        Set<LogFlag> withFlag = EnumSet.noneOf(LogFlag.class);
        withFlag.addAll(this.flags);
        withFlag.add(flag);
        return new LogValues(this.accident, this.stoolScore, withFlag, this.amountGrams, this.weightGrams, this.durationMinutes,
                this.wellbeing, this.label, this.dose, this.note, this.foods);
    }

    public static LogValues none() {
        return new LogValues(null, null, null, null, null, null, null, null, null, null, List.of());
    }

    public Set<LogField> present() {
        Set<LogField> present = EnumSet.noneOf(LogField.class);
        if (this.accident != null) present.add(LogField.ACCIDENT);
        if (this.stoolScore != null) present.add(LogField.STOOL_SCORE);
        if (!this.flags.isEmpty()) present.add(LogField.FLAGS);
        if (this.amountGrams != null) present.add(LogField.AMOUNT_GRAMS);
        if (this.weightGrams != null) present.add(LogField.WEIGHT_GRAMS);
        if (this.durationMinutes != null) present.add(LogField.DURATION_MINUTES);
        if (this.wellbeing != null) present.add(LogField.WELLBEING);
        if (this.label != null) present.add(LogField.LABEL);
        if (this.dose != null) present.add(LogField.DOSE);
        if (this.note != null) present.add(LogField.NOTE);
        if (!this.foods.isEmpty()) present.add(LogField.FOODS);
        return present;
    }

    private static String trimmedOrNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
