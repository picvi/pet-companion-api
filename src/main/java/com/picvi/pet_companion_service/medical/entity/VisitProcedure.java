package com.picvi.pet_companion_service.medical.entity;

import com.picvi.pet_companion_service.medical.domain.VisitProcedureKind;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Embeddable
@Getter
public class VisitProcedure {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitProcedureKind kind;

    private String note;

    public VisitProcedure() {}

    public VisitProcedure(VisitProcedureKind kind, String note) {
        this.kind = kind;
        this.note = note;
    }
}
