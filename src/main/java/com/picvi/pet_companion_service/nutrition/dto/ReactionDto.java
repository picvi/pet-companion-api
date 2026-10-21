package com.picvi.pet_companion_service.nutrition.dto;

import com.picvi.pet_companion_service.nutrition.domain.ReactionKind;

import java.util.UUID;

/**
 * How the tummy reacted during the change. The app shows it until the owner marks it seen. EXTEND_STEP: the step was
 * slowed down; SUGGEST_PAUSE: a second loose stool in the step; PAUSE_AND_VET: the plan is paused, see a vet.
 */
public record ReactionDto(ReactionKind kind, UUID logId, int stepOrder, boolean seen) {
}
