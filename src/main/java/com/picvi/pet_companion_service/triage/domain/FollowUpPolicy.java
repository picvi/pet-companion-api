package com.picvi.pet_companion_service.triage.domain;

public class FollowUpPolicy {

    public FollowUpOutcome apply(Level current, FollowUpAnswer answer, int sameInARow, int originalCheckBackMinutes) {
        if (answer == FollowUpAnswer.BETTER) {
            return new FollowUpOutcome(current, CheckStatus.RESOLVED, null, null);
        }
        if (answer == FollowUpAnswer.SAME) {
            if (sameInARow >= TriageLimits.MAX_SAME_BEFORE_CALL - 1) {
                return new FollowUpOutcome(Level.CALL_TODAY, CheckStatus.ESCALATED, FollowUpReason.NOT_BETTER, null);
            }
            int repeatedMinutes = Math.max(TriageLimits.MIN_REPEAT_MINUTES, originalCheckBackMinutes / 2);
            return new FollowUpOutcome(Level.WATCH, CheckStatus.WATCHING, null, repeatedMinutes);
        }
        Level raised = current == Level.GO_NOW ? Level.GO_NOW : Level.values()[current.ordinal() + 1];
        return new FollowUpOutcome(raised, CheckStatus.ESCALATED, FollowUpReason.WORSE_AFTER_WATCH, null);
    }
}
