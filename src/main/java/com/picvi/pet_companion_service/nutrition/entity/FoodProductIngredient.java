package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.IngredientRole;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Entity
@Table(name = "food_product_ingredient")
@IdClass(FoodProductIngredientId.class)
public class FoodProductIngredient {
    @Id
    @Getter
    @Column(name = "product_id")
    private UUID productId;

    @Id
    @Getter
    @Column(name = "source_code")
    private String sourceCode;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngredientRole role;

    @Getter
    @Column(nullable = false)
    private boolean hydrolysed;

    public FoodProductIngredient() {}

    public FoodProductIngredient(UUID productId, String sourceCode, IngredientRole role, boolean hydrolysed) {
        this.productId = productId;
        this.sourceCode = sourceCode;
        this.role = role;
        this.hydrolysed = hydrolysed;
    }
}
