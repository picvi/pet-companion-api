package com.picvi.pet_companion_service.development.entity;

import com.picvi.pet_companion_service.development.domain.ActivityCategory;
import com.picvi.pet_companion_service.development.domain.ActivityDefinition;
import com.picvi.pet_companion_service.development.domain.ActivityKind;
import com.picvi.pet_companion_service.development.domain.VaccinationRequirement;
import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;


@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "development_activity")
public class DevelopmentActivity {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(nullable = false, unique = true)
    private String code;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityKind kind;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityCategory category;

    @Getter
    @Column(name = "start_age_weeks", nullable = false)
    private int startAgeWeeks;

    @Getter
    @Column(name = "end_age_weeks", nullable = false)
    private int endAgeWeeks;

    @Getter
    @Column(nullable = false)
    private int difficulty;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "vaccination_requirement", nullable = false)
    private VaccinationRequirement vaccinationRequirement;

    @Getter
    @Column(name = "repeat_every_days")
    private Integer repeatEveryDays;

    @Getter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Getter
    @Column(nullable = false)
    private boolean active;

    @Getter
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Getter
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public DevelopmentActivity() {}

    public ActivityDefinition toDefinition() {
        return new ActivityDefinition(code, kind, category, startAgeWeeks, endAgeWeeks, difficulty,
                vaccinationRequirement, repeatEveryDays, sortOrder);
    }
}
