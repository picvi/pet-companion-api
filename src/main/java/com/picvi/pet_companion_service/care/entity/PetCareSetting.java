package com.picvi.pet_companion_service.care.entity;

import com.picvi.pet_companion_service.pet.entity.Pet;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pet_care_setting")
public class PetCareSetting {
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

    @Setter
    @Getter
    @Column(name = "reminders_enabled", nullable = false)
    private boolean remindersEnabled;

    @Setter
    @Getter
    @Column(name = "paused_until")
    private LocalDate pausedUntil;

    @Setter
    @Getter
    @Column(name = "interval_days_override")
    private Integer intervalDaysOverride;

    public PetCareSetting() {}

    public PetCareSetting(Pet pet, CareItem item) {
        this.pet = pet;
        this.item = item;
    }
}
