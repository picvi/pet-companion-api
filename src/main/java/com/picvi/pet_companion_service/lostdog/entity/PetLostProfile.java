package com.picvi.pet_companion_service.lostdog.entity;

import com.picvi.pet_companion_service.lostdog.domain.ChipRegistry;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pet_lost_profile")
public class PetLostProfile {
    @Id
    @Getter
    @Column(name = "pet_id")
    private UUID petId;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "chip_registry", length = 24)
    private ChipRegistry chipRegistry;

    @Getter @Setter
    @Column(name = "chip_registry_name", length = 100)
    private String chipRegistryName;

    @Getter @Setter
    @Column(length = 300)
    private String markings;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PetLostProfile() {}

    public PetLostProfile(UUID petId, Instant now) {
        this.petId = petId;
        this.updatedAt = now;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
