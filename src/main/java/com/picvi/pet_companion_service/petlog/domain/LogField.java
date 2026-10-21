package com.picvi.pet_companion_service.petlog.domain;

import lombok.Getter;

@Getter
public enum LogField {
    DETAIL("detail"),
    ACCIDENT("accident"),
    STOOL_SCORE("stoolScore"),
    FLAGS("flags"),
    AMOUNT_GRAMS("amountGrams"),
    WEIGHT_GRAMS("weightGrams"),
    DURATION_MINUTES("durationMinutes"),
    WELLBEING("wellbeing"),
    LABEL("label"),
    DOSE("dose"),
    NOTE("note"),
    FOODS("foods"),
    UTC_OFFSET_MINUTES("utcOffsetMinutes");

    private final String jsonName;

    LogField(String jsonName) {
        this.jsonName = jsonName;
    }
}
