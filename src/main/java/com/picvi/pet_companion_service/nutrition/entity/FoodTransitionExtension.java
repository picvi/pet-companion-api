package com.picvi.pet_companion_service.nutrition.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_transition_extension")
public class FoodTransitionExtension {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "transition_id", nullable = false, updatable = false)
    private UUID transitionId;

    @Getter
    @Column(name = "step_order", nullable = false, updatable = false)
    private int stepOrder;

    @Getter
    @Column(nullable = false)
    private int days;

    @Getter
    @Column(name = "reason_log_id", updatable = false)
    private UUID reasonLogId;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public FoodTransitionExtension() {}

    /** Undone: no extra days, but the step still counts as slowed down once, so nothing adds it again. */
    public void tombstone() {
        this.days = 0;
    }

    public boolean isTombstone() {
        return this.days == 0;
    }

    public void restore(int days) {
        this.days = days;
    }

    public FoodTransitionExtension(UUID transitionId, int stepOrder, int days, UUID reasonLogId, OffsetDateTime createdAt) {
        this.transitionId = transitionId;
        this.stepOrder = stepOrder;
        this.days = days;
        this.reasonLogId = reasonLogId;
        this.createdAt = createdAt;
    }
}
