package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.SafetyLevel;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.List;

@Entity
@Immutable
@Table(name = "food_safety_item")
public class FoodSafetyItem {
    @Id
    @Getter
    private String code;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SafetyLevel level;

    @Getter
    @Column(name = "quick_order")
    private Integer quickOrder;

    @Getter
    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Getter
    @Column(name = "name_pl", nullable = false)
    private String namePl;

    @Getter
    @Column(name = "why_en", nullable = false)
    private String whyEn;

    @Getter
    @Column(name = "why_pl", nullable = false)
    private String whyPl;

    @Getter
    @Column(name = "how_en")
    private String howEn;

    @Getter
    @Column(name = "how_pl")
    private String howPl;

    @Getter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "aliases_en", nullable = false)
    private List<String> aliasesEn;

    @Getter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "aliases_pl", nullable = false)
    private List<String> aliasesPl;

    @Getter
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private List<String> sources;

    @Getter
    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Getter
    @Column(name = "reviewed_on")
    private LocalDate reviewedOn;

    public FoodSafetyItem() {}

    public boolean isReviewed() {
        return this.reviewedOn != null;
    }
}
