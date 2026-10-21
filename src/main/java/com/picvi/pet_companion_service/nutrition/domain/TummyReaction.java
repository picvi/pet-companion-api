package com.picvi.pet_companion_service.nutrition.domain;

import java.util.UUID;

/** What to do about the tummy, and the log that caused it (null for NONE). */
public record TummyReaction(ReactionKind kind, UUID logId) {
    public static final TummyReaction NONE = new TummyReaction(ReactionKind.NONE, null);
}
