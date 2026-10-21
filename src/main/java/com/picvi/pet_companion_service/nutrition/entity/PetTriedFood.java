package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.TriedReaction;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pet_tried_food")
@IdClass(PetTriedFoodId.class)
public class PetTriedFood {
    @Id
    @Getter
    @Column(name = "pet_id", updatable = false)
    private UUID petId;

    @Id
    @Getter
    @Column(name = "food_code", updatable = false)
    private String foodCode;

    @Getter
    @Column(name = "tried_on", nullable = false)
    private LocalDate triedOn;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriedReaction reaction;

    @Getter
    private String note;

    @Getter
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PetTriedFood() {}

    public PetTriedFood(UUID petId, String foodCode, LocalDate triedOn, TriedReaction reaction, String note, UUID createdBy,
                        Instant now) {
        this.petId = petId;
        this.foodCode = foodCode;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.update(triedOn, reaction, note, now);
    }

    public void update(LocalDate triedOn, TriedReaction reaction, String note, Instant now) {
        this.triedOn = triedOn;
        this.reaction = reaction;
        this.note = note;
        this.updatedAt = now;
    }
}
