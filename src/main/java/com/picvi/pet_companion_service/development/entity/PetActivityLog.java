package com.picvi.pet_companion_service.development.entity;

import com.picvi.pet_companion_service.development.domain.Outcome;
import com.picvi.pet_companion_service.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.util.UUID;
import java.time.Instant;

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "pet_activity_log")
public class PetActivityLog {
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

    @Getter
    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Getter
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PetActivityLog() {}

    public PetActivityLog(Pet pet, DevelopmentActivity activity, LocalDate performedOn, Outcome outcome, Integer durationSeconds) {
        this.pet = pet;
        this.activity = activity;
        this.performedOn = performedOn;
        this.outcome = outcome;
        this.durationSeconds = durationSeconds;
    }
}
