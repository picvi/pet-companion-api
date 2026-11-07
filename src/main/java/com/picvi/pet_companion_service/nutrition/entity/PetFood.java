package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.PetFoodStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pet_food")
public class PetFood {
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private FoodProduct product;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodKind kind;

    @Getter
    @Column(name = "daily_grams")
    private Integer dailyGrams;

    @Getter
    @Column(name = "for_training", nullable = false)
    private boolean forTraining;

    @Getter
    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Getter
    @Column(name = "ended_on")
    private LocalDate endedOn;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetFoodStatus status;

    @Getter
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Version
    @Getter
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PetFood() {}

    public PetFood(UUID petId, UUID clientId, FoodProduct product, FoodKind kind, Integer dailyGrams, boolean forTraining,
                   LocalDate startedOn, UUID createdBy, Instant now) {
        this.petId = petId;
        this.clientId = clientId;
        this.product = product;
        this.kind = kind;
        this.dailyGrams = dailyGrams;
        this.forTraining = forTraining;
        this.startedOn = startedOn;
        this.status = PetFoodStatus.ACTIVE;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    /** True on the days the food was given: from startedOn up to, not including, endedOn. */
    public boolean isGivenOn(LocalDate date) {
        return this.status != PetFoodStatus.ENTERED_IN_ERROR
                && !date.isBefore(this.startedOn)
                && (this.endedOn == null || date.isBefore(this.endedOn));
    }

    public void update(Integer dailyGrams, boolean forTraining) {
        this.dailyGrams = dailyGrams;
        this.forTraining = forTraining;
    }

    /** The first day the food is no longer given. */
    public void endOn(LocalDate date) {
        this.status = PetFoodStatus.ENDED;
        this.endedOn = date.isBefore(this.startedOn) ? this.startedOn : date;
    }

    public void markEnteredInError() {
        this.status = PetFoodStatus.ENTERED_IN_ERROR;
    }
}
