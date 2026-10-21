package com.picvi.pet_companion_service.nutrition.entity;

import com.picvi.pet_companion_service.nutrition.domain.ChangeReason;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/** What the admin changed in a product, with the values before and after. The audit behind every correction. */
@Entity
@Table(name = "food_product_change")
public class FoodProductChange {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Getter
    @Column(name = "changed_by", updatable = false)
    private UUID changedBy;

    @Getter
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ChangeReason reason;

    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(updatable = false)
    private Map<String, Object> before;

    @Getter
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after", updatable = false)
    private Map<String, Object> after;

    public FoodProductChange() {}

    public FoodProductChange(UUID productId, UUID changedBy, OffsetDateTime changedAt, ChangeReason reason,
                             Map<String, Object> before, Map<String, Object> after) {
        this.productId = productId;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.reason = reason;
        this.before = before;
        this.after = after;
    }
}
