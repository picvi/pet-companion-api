package com.picvi.pet_companion_service.nutrition.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "food_transition_pace")
@IdClass(FoodTransitionPaceRowId.class)
public class FoodTransitionPaceRow {
    @Id
    @Getter
    private String pace;

    @Id
    @Getter
    @Column(name = "step_order")
    private int stepOrder;

    @Getter
    private int days;

    @Getter
    @Column(name = "new_percent")
    private int newPercent;

    @Getter
    private String status;

    public FoodTransitionPaceRow() {}
}
