package com.picvi.pet_companion_service.nutrition.domain;

import com.picvi.pet_companion_service.nutrition.domain.ReactionSignal.Kind;

import java.util.List;
import java.util.Set;

/**
 * During a food change: red flags pause the plan and send the owner to the vet; the first loose stool of a step slows
 * the step down; a second loose stool in the same step only suggests a pause.
 */
public class TummyReactionPolicy {
    private static final Set<Kind> RED_FLAGS = Set.of(Kind.VOMITING, Kind.BLOOD, Kind.VERY_UNWELL);
    public static final int REFUSED_MEALS_IN_A_ROW = 2;

    /**
     * @param stepSignals signals from the logs since the current step began, oldest first
     * @param lastMeals   the latest meals of the change, newest first
     */
    public TummyReaction evaluate(List<ReactionSignal> stepSignals, List<ReactionSignal> lastMeals) {
        ReactionSignal redFlag = last(stepSignals, RED_FLAGS);
        if (redFlag != null) {
            return new TummyReaction(ReactionKind.PAUSE_AND_VET, redFlag.logId());
        }
        if (lastMeals.size() >= REFUSED_MEALS_IN_A_ROW
                && lastMeals.stream().limit(REFUSED_MEALS_IN_A_ROW).allMatch(meal -> meal.kind() == Kind.MEAL_REFUSED)) {
            return new TummyReaction(ReactionKind.PAUSE_AND_VET, lastMeals.get(0).logId());
        }
        long loose = stepSignals.stream().filter(signal -> signal.kind() == Kind.LOOSE_STOOL).count();
        ReactionSignal latestLoose = last(stepSignals, Set.of(Kind.LOOSE_STOOL));
        if (loose >= 2) {
            return new TummyReaction(ReactionKind.SUGGEST_PAUSE, latestLoose.logId());
        }
        return loose == 1 ? new TummyReaction(ReactionKind.EXTEND_STEP, latestLoose.logId()) : TummyReaction.NONE;
    }

    private static ReactionSignal last(List<ReactionSignal> signals, Set<Kind> kinds) {
        ReactionSignal found = null;
        for (ReactionSignal signal : signals) {
            if (kinds.contains(signal.kind())) {
                found = signal;
            }
        }
        return found;
    }
}
