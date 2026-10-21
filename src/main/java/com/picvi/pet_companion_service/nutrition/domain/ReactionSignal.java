package com.picvi.pet_companion_service.nutrition.domain;

import java.util.UUID;

/** What a log says about the tummy, reduced to what the policy needs. */
public record ReactionSignal(UUID logId, Kind kind) {

    public enum Kind {
        LOOSE_STOOL, VOMITING, BLOOD, VERY_UNWELL, MEAL_REFUSED, MEAL_EATEN
    }
}
