package com.picvi.pet_companion_service.care.entity;

import com.picvi.pet_companion_service.care.domain.CareCategory;
import com.picvi.pet_companion_service.care.domain.CareRequirement;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "care_item")
public class CareItem {
    @Id
    @Getter
    private String code;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareCategory category;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareRequirement requirement;

    @Getter
    @Column(name = "interval_editable", nullable = false)
    private boolean intervalEditable;

    @Getter
    @Column(name = "drives_vaccination_status", nullable = false)
    private boolean drivesVaccinationStatus;

    @Getter
    @Column(name = "legal_min_age_months")
    private Integer legalMinAgeMonths;

    @Getter
    @Column(name = "legal_window_days")
    private Integer legalWindowDays;

    @Getter
    @Column(name = "legal_repeat_months")
    private Integer legalRepeatMonths;

    @Getter
    @Column(nullable = false)
    private boolean active;

    @Getter
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public CareItem() {}
}
