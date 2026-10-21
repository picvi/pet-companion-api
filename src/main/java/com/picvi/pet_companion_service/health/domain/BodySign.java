package com.picvi.pet_companion_service.health.domain;

import java.util.List;

public enum BodySign {
    EATING, POO, ENERGY, SLEEP, VOMIT, SKIN, DRINKING, BREATHING, OTHER;

    public static List<BodySign> always() {
        return List.of(EATING, POO, ENERGY, SLEEP);
    }
}
