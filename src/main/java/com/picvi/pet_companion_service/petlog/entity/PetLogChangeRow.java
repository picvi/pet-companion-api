package com.picvi.pet_companion_service.petlog.entity;

import com.picvi.pet_companion_service.petlog.domain.LogChange;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "pet_log_change")
public class PetLogChangeRow {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "log_id", nullable = false)
    private UUID logId;

    @Getter
    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "change", nullable = false)
    private LogChange change;

    @Getter
    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    public PetLogChangeRow() {}

    public PetLogChangeRow(UUID logId, UUID petId, LogChange change, OffsetDateTime changedAt) {
        this.logId = logId;
        this.petId = petId;
        this.change = change;
        this.changedAt = changedAt;
    }
}
