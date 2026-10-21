package com.picvi.pet_companion_service.lostdog.entity;

import com.picvi.pet_companion_service.lostdog.domain.LostStepCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
public class LostReportStepId implements Serializable {
    @Column(name = "report_id")
    private UUID reportId;

    @Column(name = "step_code", length = 32)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private LostStepCode stepCode;

    protected LostReportStepId() {}

    public LostReportStepId(UUID reportId, LostStepCode stepCode) {
        this.reportId = reportId;
        this.stepCode = stepCode;
    }
}
