package com.picvi.pet_companion_service.nutrition.domain;

/** Whether to ask "What does she eat?" when no main food is set. Asked, then once more after a few meals, then never. */
public class FoodAskPolicy {
    public static final int REASK_AFTER_MEALS = 3;

    /** @return ASK or NONE */
    public PortionState state(boolean dismissed, boolean dismissedAgain, long mealsLoggedSinceDismissal) {
        if (dismissedAgain) {
            return PortionState.NONE;
        }
        if (!dismissed) {
            return PortionState.ASK;
        }
        return mealsLoggedSinceDismissal >= REASK_AFTER_MEALS ? PortionState.ASK : PortionState.NONE;
    }
}
