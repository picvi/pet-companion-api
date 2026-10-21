package com.picvi.pet_companion_service.condition.entity;

import com.picvi.pet_companion_service.condition.domain.ConditionCertainty;
import com.picvi.pet_companion_service.condition.domain.ConditionKind;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pet_condition")
public class PetCondition {
    @Id
    @Getter
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Getter
    @Column(name = "pet_id", nullable = false, updatable = false)
    private UUID petId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionKind kind;

    @Getter
    @Column(name = "ingredient_code")
    private String ingredientCode;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConditionCertainty certainty;

    @Getter
    private String text;

    @Getter
    private LocalDate since;

    @Getter
    @Column(name = "resolved_on")
    private LocalDate resolvedOn;

    @Version
    @Getter
    private long version;

    @Getter
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public PetCondition() {}

    public PetCondition(UUID petId, ConditionKind kind, String ingredientCode, ConditionCertainty certainty, String text,
                        LocalDate since, UUID createdBy, Instant now) {
        this.petId = petId;
        this.kind = kind;
        this.ingredientCode = ingredientCode;
        this.certainty = certainty;
        this.text = text;
        this.since = since;
        this.createdBy = createdBy;
        this.createdAt = now;
    }

    public void update(ConditionKind kind, String ingredientCode, ConditionCertainty certainty, String text, LocalDate since,
                       LocalDate resolvedOn) {
        this.kind = kind;
        this.ingredientCode = ingredientCode;
        this.certainty = certainty;
        this.text = text;
        this.since = since;
        this.resolvedOn = resolvedOn;
    }

    /** Still counts on the given day: not resolved, or resolved after it. */
    public boolean isOpenOn(LocalDate date) {
        return this.resolvedOn == null || this.resolvedOn.isAfter(date);
    }
}
