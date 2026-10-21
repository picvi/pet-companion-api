package com.picvi.pet_companion_service.triage.entity;

import com.picvi.pet_companion_service.triage.domain.FollowUpAnswer;
import com.picvi.pet_companion_service.triage.domain.Level;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pet_check_follow_up")
public class PetCheckFollowUp {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "check_id", nullable = false, updatable = false)
    private UUID checkId;

    @Column(name = "client_id", nullable = false, updatable = false)
    private UUID clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private FollowUpAnswer answer;

    @Enumerated(EnumType.STRING)
    @Column(name = "level_after", nullable = false, updatable = false)
    private Level levelAfter;

    @Column(name = "answered_on", nullable = false, updatable = false)
    private LocalDate answeredOn;

    @Column(name = "minute_of_day", nullable = false, updatable = false)
    private int minuteOfDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PetCheckFollowUp() {
    }

    public PetCheckFollowUp(UUID checkId, UUID clientId, FollowUpAnswer answer, Level levelAfter, LocalDate answeredOn,
                            int minuteOfDay, Instant createdAt) {
        this.checkId = checkId;
        this.clientId = clientId;
        this.answer = answer;
        this.levelAfter = levelAfter;
        this.answeredOn = answeredOn;
        this.minuteOfDay = minuteOfDay;
        this.createdAt = createdAt;
    }

    public UUID getId() { return this.id; }
    public UUID getCheckId() { return this.checkId; }
    public UUID getClientId() { return this.clientId; }
    public FollowUpAnswer getAnswer() { return this.answer; }
    public Level getLevelAfter() { return this.levelAfter; }
    public LocalDate getAnsweredOn() { return this.answeredOn; }
    public int getMinuteOfDay() { return this.minuteOfDay; }
    public Instant getCreatedAt() { return this.createdAt; }
}
