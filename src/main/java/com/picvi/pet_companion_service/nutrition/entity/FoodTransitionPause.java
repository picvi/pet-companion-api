package com.picvi.pet_companion_service.nutrition.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "food_transition_pause")
public class FoodTransitionPause {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "transition_id", nullable = false, updatable = false)
    private UUID transitionId;

    @Getter
    @Column(name = "paused_on", nullable = false, updatable = false)
    private LocalDate pausedOn;

    @Getter
    @Column(name = "resumed_on")
    private LocalDate resumedOn;

    public FoodTransitionPause() {}

    public FoodTransitionPause(UUID transitionId, LocalDate pausedOn) {
        this.transitionId = transitionId;
        this.pausedOn = pausedOn;
    }

    public boolean isOpen() {
        return this.resumedOn == null;
    }

    public void resumeOn(LocalDate date) {
        this.resumedOn = date.isBefore(this.pausedOn) ? this.pausedOn : date;
    }
}
