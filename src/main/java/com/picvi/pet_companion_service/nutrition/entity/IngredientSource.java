package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.IngredientKind;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ingredient_source")
public class IngredientSource {
    @Id
    @Getter
    private String code;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IngredientKind kind;

    @Getter
    @Column(name = "parent_code")
    private String parentCode;

    @Getter
    @Column(name = "allergen_relevant", nullable = false)
    private boolean allergenRelevant;

    @Getter
    @Column(nullable = false)
    private String status;

    @Getter
    @Column(name = "replaced_by")
    private String replacedBy;

    @Getter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public IngredientSource() {}
}
