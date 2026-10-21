package com.picvi.pet_companion_service.petlog.domain;

import lombok.Getter;

@Getter
public enum LogDetail {
    PEE(LogType.TOILET),
    POO(LogType.TOILET),
    BOTH(LogType.TOILET),
    ACCIDENT(LogType.TOILET, true),
    ATE_ALL(LogType.MEAL),
    LEFT_SOME(LogType.MEAL),
    REFUSED(LogType.MEAL),
    VOMITING(LogType.SYMPTOM),
    NOT_EATING(LogType.SYMPTOM),
    DRINKING_MORE(LogType.SYMPTOM),
    SWALLOWED_OBJECT(LogType.SYMPTOM),
    SCOOTING(LogType.SYMPTOM),
    LOW_ENERGY(LogType.SYMPTOM),
    WEAK_WOBBLY(LogType.SYMPTOM),
    COUGHING(LogType.SYMPTOM),
    SNEEZING(LogType.SYMPTOM),
    BREATHING_HARD(LogType.SYMPTOM),
    SCRATCHING(LogType.SYMPTOM),
    SKIN(LogType.SYMPTOM),
    PARASITE_FOUND(LogType.SYMPTOM),
    SWELLING(LogType.SYMPTOM),
    EYE(LogType.SYMPTOM),
    EAR(LogType.SYMPTOM),
    LIMPING(LogType.SYMPTOM),
    PAIN(LogType.SYMPTOM),
    URINARY(LogType.SYMPTOM),
    OTHER(LogType.SYMPTOM);

    private final LogType type;
    private final boolean retired;

    LogDetail(LogType type) {
        this(type, false);
    }

    LogDetail(LogType type, boolean retired) {
        this.type = type;
        this.retired = retired;
    }
}
