package com.picvi.pet_companion_service.lostdog.entity;

import com.picvi.pet_companion_service.lostdog.domain.LostStepCode;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_report_step")
public class LostReportStep {
    @EmbeddedId @Getter
    private LostReportStepId id;

    @Getter
    @Column(name = "done_at")
    private Instant doneAt;

    protected LostReportStep() {}

    public LostReportStep(UUID reportId, LostStepCode code, Instant doneAt) {
        this.id = new LostReportStepId(reportId, code);
        this.doneAt = doneAt;
    }

    public void setDone(boolean done, Instant now) {
        this.doneAt = done ? now : null;
    }
}
