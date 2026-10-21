package com.picvi.pet_companion_service.petlog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.util.UUID;

@Embeddable
public class LoggedFood {
    @Getter
    @Column(name = "pet_food_id", nullable = false)
    private UUID petFoodId;

    @Getter
    private Integer grams;

    public LoggedFood() {}

    public LoggedFood(UUID petFoodId, Integer grams) {
        this.petFoodId = petFoodId;
        this.grams = grams;
    }
}
