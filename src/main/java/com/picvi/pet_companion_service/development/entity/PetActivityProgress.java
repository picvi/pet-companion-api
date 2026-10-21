package com.picvi.pet_companion_service.development.entity;

import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.development.domain.ProgressState;
import com.picvi.pet_companion_service.development.domain.ProgressStatus;
import com.picvi.pet_companion_service.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "pet_activity_progress")
public class PetActivityProgress {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_id", nullable = false)
    private DevelopmentActivity activity;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProgressStatus status = ProgressStatus.NOT_STARTED;

    @Setter
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "last_outcome")
    private Outcome lastOutcome;

    @Setter
    @Getter
    @Column(name = "attempt_days", nullable = false)
    private int attemptDays;

    @Setter
    @Getter
    @Column(name = "first_attempt_on")
    private LocalDate firstAttemptOn;

    @Setter
    @Getter
    @Column(name = "last_attempt_on")
    private LocalDate lastAttemptOn;

    @Setter
    @Getter
    @Column(name = "completed_on")
    private LocalDate completedOn;

    @Setter
    @Getter
    @Column(name = "snoozed_until")
    private LocalDate snoozedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PetActivityProgress() {}

    public PetActivityProgress(Pet pet, DevelopmentActivity activity) {
        this.pet = pet;
        this.activity = activity;
    }

    public ProgressState toState() {
        return new ProgressState(status, lastOutcome, attemptDays, firstAttemptOn, lastAttemptOn, completedOn, snoozedUntil);
    }

    public void apply(ProgressState state) {
        this.status = state.status();
        this.lastOutcome = state.lastOutcome();
        this.attemptDays = state.attemptDays();
        this.firstAttemptOn = state.firstAttemptOn();
        this.lastAttemptOn = state.lastAttemptOn();
        this.completedOn = state.completedOn();
        this.snoozedUntil = state.snoozedUntil();
    }
}
