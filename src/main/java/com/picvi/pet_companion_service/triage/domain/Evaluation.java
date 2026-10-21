package com.picvi.pet_companion_service.triage.domain;

public record Evaluation(Rule winner) {
    public Level level() {
        return this.winner.level();
    }
}
