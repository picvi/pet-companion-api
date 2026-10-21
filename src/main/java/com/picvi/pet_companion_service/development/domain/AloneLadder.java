package com.picvi.pet_companion_service.development.domain;

import com.picvi.pet_companion_service.exception.development.InvalidOutcome;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "stay alone" ladder: how long the puppy is left, and what to try next.
 * Calm moves one step up, unsure stays, upset goes one step down. The goal is never stored.
 */
public final class AloneLadder {
    public static final List<Integer> STEPS_SECONDS =
            List.of(5, 10, 20, 30, 45, 60, 120, 300, 600, 900, 1200, 1800, 2700, 3600);

    private static final Map<String, Integer> LEARNED_AFTER_SECONDS =
            Map.of("ALONE_FIRST_SECONDS", 60, "ALONE_BUILD_MINUTES", 30 * 60);
    private static final Set<Outcome> REACTIONS = Set.of(Outcome.CALM, Outcome.UNSURE, Outcome.SCARED);

    private AloneLadder() {}

    public static boolean isAloneActivity(String code) {
        return LEARNED_AFTER_SECONDS.containsKey(code);
    }

    public static int learnedAfterSeconds(String code) {
        return LEARNED_AFTER_SECONDS.get(code);
    }

    public static int firstGoalSeconds() {
        return STEPS_SECONDS.get(0);
    }

    public static void requireReaction(Outcome reaction) {
        if (!REACTIONS.contains(reaction)) {
            throw new InvalidOutcome();
        }
    }

    public static int suggestNext(int lastSeconds, Outcome reaction) {
        requireReaction(reaction);
        int step = stepIndexOf(lastSeconds);
        int next = switch (reaction) {
            case CALM -> Math.min(step + 1, STEPS_SECONDS.size() - 1);
            case SCARED -> Math.max(step - 1, 0);
            default -> step;
        };
        return STEPS_SECONDS.get(next);
    }

    /** Every session counts as practice; a calm one at or past the threshold means learned. */
    public static Outcome progressOutcome(String code, int seconds, Outcome reaction) {
        requireReaction(reaction);
        boolean learned = reaction == Outcome.CALM && seconds >= learnedAfterSeconds(code);
        return learned ? Outcome.LEARNED : Outcome.PRACTISED;
    }

    private static int stepIndexOf(int seconds) {
        int index = 0;
        for (int i = 0; i < STEPS_SECONDS.size(); i++) {
            if (STEPS_SECONDS.get(i) <= seconds) {
                index = i;
            }
        }
        return index;
    }
}
