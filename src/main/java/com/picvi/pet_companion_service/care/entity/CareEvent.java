package com.picvi.pet_companion_service.care.entity;

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
@Table(name = "care_event")
public class CareEvent {
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
    @JoinColumn(name = "item_code", nullable = false)
    private CareItem item;

    @Getter
    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CareEvent() {}

    public CareEvent(Pet pet, CareItem item, LocalDate performedOn) {
        this.pet = pet;
        this.item = item;
        this.performedOn = performedOn;
    }
}
