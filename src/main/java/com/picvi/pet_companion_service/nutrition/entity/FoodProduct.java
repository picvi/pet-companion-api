package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.BreedSize;
import com.picvi.pet_companion_service.nutrition.domain.Completeness;
import com.picvi.pet_companion_service.nutrition.domain.FoodForm;
import com.picvi.pet_companion_service.nutrition.domain.FoodKind;
import com.picvi.pet_companion_service.nutrition.domain.LifeStage;
import com.picvi.pet_companion_service.nutrition.domain.ProductStatus;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "food_product")
public class FoodProduct {
    @Id
    @Getter
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Getter
    private String brand;

    @Getter
    @Column(name = "product_line")
    private String productLine;

    @Getter
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Getter
    private String flavour;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodForm form;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "kind_hint", nullable = false)
    private FoodKind kindHint;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "life_stage", nullable = false)
    private LifeStage lifeStage;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "breed_size", nullable = false)
    private BreedSize breedSize;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Completeness complete;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "protein_status", nullable = false)
    private ProteinStatus proteinStatus;

    @Getter
    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Getter
    @Setter
    @Column(name = "merged_into_id")
    private UUID mergedIntoId;

    @Getter
    @Column(name = "normalised_name", nullable = false)
    private String normalisedName;

    @Getter
    @Column(name = "source_url")
    private String sourceUrl;

    @Getter
    @Column(name = "checked_on")
    private LocalDate checkedOn;

    @Getter
    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Getter
    @Setter
    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Getter
    @Setter
    @Column(name = "reviewed_on")
    private LocalDate reviewedOn;

    @Getter
    @Setter
    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Version
    @Getter
    private long version;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public FoodProduct() {}

    /** The admin's reviewed version: corrected details, public from now on. */
    public void approve(String brand, String productLine, String productName, String flavour, FoodForm form, FoodKind kind,
                        LifeStage lifeStage, BreedSize breedSize, Completeness complete, ProteinStatus proteinStatus,
                        String normalisedName, String sourceUrl, UUID reviewer, LocalDate on, Instant now) {
        this.brand = brand;
        this.productLine = productLine;
        this.productName = productName;
        this.flavour = flavour;
        this.form = form;
        this.kindHint = kind;
        this.lifeStage = lifeStage;
        this.breedSize = breedSize;
        this.complete = complete;
        this.proteinStatus = proteinStatus;
        this.normalisedName = normalisedName;
        this.sourceUrl = sourceUrl;
        this.status = ProductStatus.PUBLIC;
        this.reviewedBy = reviewer;
        this.reviewedOn = on;
        this.updatedAt = now;
    }

    public void mergeInto(UUID targetId, UUID reviewer, LocalDate on, Instant now) {
        this.status = ProductStatus.MERGED;
        this.mergedIntoId = targetId;
        this.reviewedBy = reviewer;
        this.reviewedOn = on;
        this.updatedAt = now;
    }

    public void reject(UUID reviewer, LocalDate on, Instant now) {
        this.status = ProductStatus.REJECTED;
        this.reviewedBy = reviewer;
        this.reviewedOn = on;
        this.updatedAt = now;
    }

    /** A product an owner typed in. Visible only to its creator until the admin reviews it. */
    public static FoodProduct ownerAdded(UUID ownerId, String brand, String productName, String flavour, FoodForm form,
                                         FoodKind kind, LifeStage lifeStage, Completeness complete,
                                         ProteinStatus proteinStatus, String normalisedName, Instant now) {
        FoodProduct product = new FoodProduct();
        product.createdBy = ownerId;
        product.brand = brand;
        product.productName = productName;
        product.flavour = flavour;
        product.form = form;
        product.kindHint = kind;
        product.lifeStage = lifeStage;
        product.breedSize = BreedSize.ALL;
        product.complete = complete;
        product.proteinStatus = proteinStatus;
        product.status = ProductStatus.OWNER_ADDED;
        product.normalisedName = normalisedName;
        product.createdAt = now;
        product.updatedAt = now;
        return product;
    }
}
