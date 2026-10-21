package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.TransitionEndReason;
import com.picvi.pet_companion_service.nutrition.domain.TransitionPace;
import com.picvi.pet_companion_service.nutrition.domain.TransitionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_transition")
public class FoodTransition {
    @Id
    @Getter
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Getter
    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    @Getter
    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Getter
    @Column(name = "from_food_id", nullable = false, updatable = false)
    private UUID fromFoodId;

    @Getter
    @Column(name = "to_food_id", nullable = false, updatable = false)
    private UUID toFoodId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private TransitionPace pace;

    @Getter
    @Column(name = "start_on", nullable = false, updatable = false)
    private LocalDate startOn;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransitionStatus status;

    @Getter
    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason")
    private TransitionEndReason endReason;

    @Getter
    @Column(name = "finished_seen_at")
    private OffsetDateTime finishedSeenAt;

    @Getter
    @Column(name = "reaction_seen_log_id")
    private UUID reactionSeenLogId;

    @Version
    @Getter
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public FoodTransition() {}

    private FoodTransition(UUID petId, UUID clientId, UUID fromFoodId, UUID toFoodId, TransitionPace pace, LocalDate startOn,
                           Instant now) {
        this.petId = petId;
        this.clientId = clientId;
        this.fromFoodId = fromFoodId;
        this.toFoodId = toFoodId;
        this.pace = pace;
        this.startOn = startOn;
        this.status = TransitionStatus.ACTIVE;
        this.createdAt = now;
    }

    public static FoodTransition planned(UUID petId, UUID clientId, UUID fromFoodId, UUID toFoodId, TransitionPace pace,
                                         LocalDate startOn, Instant now) {
        return new FoodTransition(petId, clientId, fromFoodId, toFoodId, pace, startOn, now);
    }

    /** An all-at-once switch: done the day it starts, with nothing to show afterwards. */
    public static FoodTransition replacement(UUID petId, UUID clientId, UUID fromFoodId, UUID toFoodId, LocalDate on,
                                             Instant now, OffsetDateTime nowAt) {
        FoodTransition transition = new FoodTransition(petId, clientId, fromFoodId, toFoodId, TransitionPace.IMMEDIATE, on, now);
        transition.status = TransitionStatus.DONE;
        transition.endReason = TransitionEndReason.REPLACED;
        transition.endedOn = on;
        transition.finishedSeenAt = nowAt;
        return transition;
    }

    public boolean isOpen() {
        return this.status == TransitionStatus.ACTIVE || this.status == TransitionStatus.PAUSED;
    }

    public void pause() {
        this.status = TransitionStatus.PAUSED;
    }

    public void resume() {
        this.status = TransitionStatus.ACTIVE;
    }

    public void finish(LocalDate lastDay) {
        this.status = TransitionStatus.DONE;
        this.endReason = TransitionEndReason.FINISHED;
        this.endedOn = lastDay;
    }

    public void cancel(TransitionEndReason reason, LocalDate on) {
        this.status = TransitionStatus.CANCELLED;
        this.endReason = reason;
        this.endedOn = on;
    }

    public void markReactionSeen(UUID logId) {
        this.reactionSeenLogId = logId;
    }

    public void markFinishedSeen(OffsetDateTime at) {
        if (this.finishedSeenAt == null) {
            this.finishedSeenAt = at;
        }
    }
}
