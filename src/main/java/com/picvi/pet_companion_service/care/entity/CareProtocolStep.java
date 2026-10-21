package com.picvi.pet_companion_service.care.entity;

import com.picvi.pet_companion_service.care.domain.StepKind;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "care_protocol_step")
public class CareProtocolStep {
    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Getter
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepKind kind;

    @Getter
    @Column(name = "min_age_weeks")
    private Integer minAgeWeeks;

    @Getter
    @Column(name = "max_age_weeks")
    private Integer maxAgeWeeks;

    @Getter
    @Column(name = "gap_days")
    private Integer gapDays;

    @Getter
    @Column(name = "season_start_month")
    private Integer seasonStartMonth;

    @Getter
    @Column(name = "season_end_month")
    private Integer seasonEndMonth;

    public CareProtocolStep() {}
}
