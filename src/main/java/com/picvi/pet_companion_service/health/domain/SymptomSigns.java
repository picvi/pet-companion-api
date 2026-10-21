package com.picvi.pet_companion_service.health.domain;

import com.picvi.pet_companion_service.petlog.domain.LogDetail;

public final class SymptomSigns {

    private SymptomSigns() {
    }

    public static BodySign of(LogDetail detail) {
        if (detail == null) {
            return BodySign.OTHER;
        }
        return switch (detail) {
            case NOT_EATING -> BodySign.EATING;
            case LOW_ENERGY, WEAK_WOBBLY -> BodySign.ENERGY;
            case VOMITING -> BodySign.VOMIT;
            case SCRATCHING, SKIN, PARASITE_FOUND, SWELLING -> BodySign.SKIN;
            case DRINKING_MORE, URINARY -> BodySign.DRINKING;
            case COUGHING, SNEEZING, BREATHING_HARD -> BodySign.BREATHING;
            case SWALLOWED_OBJECT, SCOOTING, EYE, EAR, LIMPING, PAIN, OTHER -> BodySign.OTHER;
            case PEE, POO, BOTH, ACCIDENT, ATE_ALL, LEFT_SOME, REFUSED ->
                    throw new IllegalArgumentException(detail + " is not a symptom");
        };
    }
}
